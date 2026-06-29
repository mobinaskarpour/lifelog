package com.lifelog.service.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.lifelog.domain.model.SmsAccessStatus
import com.lifelog.domain.model.SmsMessage
import com.lifelog.domain.model.SmsMessageType
import com.lifelog.domain.model.SmsSyncStats
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed class SmsReadResult {
    data class Success(
        val messages: List<SmsMessage>,
        val providerStats: SmsSyncStats,
    ) : SmsReadResult()

    data class Failure(
        val status: SmsAccessStatus,
        val message: String,
    ) : SmsReadResult()
}

@Singleton
class SmsProviderReader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val contactCache = mutableMapOf<String, String?>()
        private var contactsPermissionGranted: Boolean? = null

        fun getAccessStatus(): SmsAccessStatus =
            when {
                !hasReadPermission() -> SmsAccessStatus.PERMISSION_DENIED
                else -> SmsAccessStatus.GRANTED
            }

        fun hasReadPermission(): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS,
            ) == PackageManager.PERMISSION_GRANTED

        private fun hasContactsPermission(): Boolean {
            contactsPermissionGranted?.let { return it }
            val granted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS,
                ) == PackageManager.PERMISSION_GRANTED
            contactsPermissionGranted = granted
            if (!granted) {
                Timber.d("Skipping contact name lookup: READ_CONTACTS not granted")
            }
            return granted
        }

        fun readAllMessages(): SmsReadResult {
            if (!hasReadPermission()) {
                return SmsReadResult.Failure(
                    SmsAccessStatus.PERMISSION_DENIED,
                    "SMS permission is required to read messages from this device.",
                )
            }

            return try {
                val messages = attachContactNames(readMergedFromUris(FULL_SYNC_URIS))
                val stats = buildStats(messages)
                Timber.d(
                    "SMS provider read: inbox=${stats.providerInboxCount} " +
                        "sent=${stats.providerSentCount} outbox=${stats.providerOutboxCount} " +
                        "draft=${stats.providerDraftCount}",
                )
                SmsReadResult.Success(messages, stats)
            } catch (security: SecurityException) {
                Timber.e(security, "SMS provider access restricted")
                SmsReadResult.Failure(
                    SmsAccessStatus.PROVIDER_RESTRICTED,
                    "Android restricted SMS access. Grant SMS permission or set LifeLog as the default SMS app.",
                )
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to read SMS provider")
                SmsReadResult.Failure(
                    SmsAccessStatus.PROVIDER_UNAVAILABLE,
                    exception.message ?: "Unable to read SMS from this device.",
                )
            }
        }

        fun readRecentOutgoing(sinceTimestamp: Long): SmsReadResult {
            if (!hasReadPermission()) {
                return SmsReadResult.Failure(
                    SmsAccessStatus.PERMISSION_DENIED,
                    "SMS permission is required to read messages from this device.",
                )
            }

            return try {
                val selection =
                    "(${Telephony.Sms.TYPE} IN (2, 4, 5, 6)) AND " +
                        "(${Telephony.Sms.DATE} >= ? OR ${Telephony.Sms.DATE_SENT} >= ?)"
                val selectionArgs = arrayOf(sinceTimestamp.toString(), sinceTimestamp.toString())
                val fromTypedFolders = readMergedFromUris(OUTGOING_FOLDER_URIS, selection, selectionArgs)
                val fromMainTable = readFromUri(Telephony.Sms.CONTENT_URI, selection, selectionArgs)
                val messages =
                    attachContactNames(
                        (fromTypedFolders + fromMainTable)
                            .distinctBy { it.providerId }
                            .sortedByDescending { it.effectiveTimestamp() },
                    )
                SmsReadResult.Success(messages, buildStats(messages))
            } catch (security: SecurityException) {
                Timber.e(security, "SMS outgoing read restricted")
                SmsReadResult.Failure(
                    SmsAccessStatus.PROVIDER_RESTRICTED,
                    "Android restricted SMS access.",
                )
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to read recent outgoing SMS")
                SmsReadResult.Failure(
                    SmsAccessStatus.PROVIDER_UNAVAILABLE,
                    exception.message ?: "Unable to read outgoing SMS.",
                )
            }
        }

        private fun readMergedFromUris(
            uris: List<Uri>,
            selection: String? = null,
            selectionArgs: Array<String>? = null,
        ): List<SmsMessage> {
            val byProviderId = linkedMapOf<Long, SmsMessage>()
            uris.forEach { uri ->
                readFromUri(uri, selection, selectionArgs).forEach { message ->
                    val existing = byProviderId[message.providerId]
                    byProviderId[message.providerId] =
                        if (existing == null || shouldPrefer(message, existing)) {
                            message
                        } else {
                            existing
                        }
                }
            }
            return byProviderId.values.sortedByDescending { it.effectiveTimestamp() }
        }

        private fun shouldPrefer(
            candidate: SmsMessage,
            existing: SmsMessage,
        ): Boolean {
            if (candidate.type == SmsMessageType.SENT && existing.type == SmsMessageType.OUTBOX) {
                return true
            }
            if (candidate.type == SmsMessageType.FAILED && existing.type == SmsMessageType.QUEUED) {
                return true
            }
            return candidate.effectiveTimestamp() >= existing.effectiveTimestamp()
        }

        private fun readFromUri(
            uri: Uri,
            selection: String? = null,
            selectionArgs: Array<String>? = null,
        ): List<SmsMessage> {
            val messages = mutableListOf<SmsMessage>()
            val cursor =
                context.contentResolver.query(
                    uri,
                    SMS_PROJECTION,
                    selection,
                    selectionArgs,
                    "${Telephony.Sms.DATE} DESC",
                ) ?: return emptyList()

            cursor.use {
                while (it.moveToNext()) {
                    mapCursorToMessage(it)?.let { message -> messages.add(message) }
                }
            }
            return messages
        }

        private fun buildStats(messages: List<SmsMessage>): SmsSyncStats =
            SmsSyncStats(
                providerInboxCount = messages.count { it.type == SmsMessageType.INBOX },
                providerSentCount = messages.count { it.type == SmsMessageType.SENT },
                providerOutboxCount = messages.count { it.type == SmsMessageType.OUTBOX },
                providerDraftCount = messages.count { it.type == SmsMessageType.DRAFT },
            )

        private fun mapCursorToMessage(cursor: Cursor): SmsMessage? {
            val providerId = cursor.getLongOrNull(COL_ID) ?: return null
            val threadId = cursor.getLongOrNull(COL_THREAD_ID) ?: 0L
            val address = cursor.getStringOrNull(COL_ADDRESS).orEmpty().ifBlank { "Unknown" }
            val body = cursor.getStringOrNull(COL_BODY).orEmpty()
            val rawDate = cursor.getLongOrNull(COL_DATE) ?: 0L
            val dateSent = cursor.getLongOrNull(COL_DATE_SENT) ?: rawDate
            val type = SmsMessageType.fromAndroidType(cursor.getIntOrNull(COL_TYPE) ?: 0)
            val date = normalizeDate(rawDate, dateSent, type)
            val read = cursor.getIntOrNull(COL_READ) == 1
            val seen = cursor.getIntOrNull(COL_SEEN) == 1
            val status = cursor.getIntOrNull(COL_STATUS) ?: 0
            val subscriptionId = cursor.getIntOrNull(COL_SUBSCRIPTION_ID) ?: -1
            val serviceCenter = cursor.getStringOrNull(COL_SERVICE_CENTER)
            val person = cursor.getLongOrNull(COL_PERSON)

            return SmsMessage(
                providerId = providerId,
                threadId = threadId,
                address = address,
                contactName = null,
                body = body,
                date = date,
                dateSent = dateSent,
                type = type,
                read = read,
                seen = seen,
                status = status,
                subscriptionId = subscriptionId,
                serviceCenter = serviceCenter,
                person = person,
            )
        }

        private fun normalizeDate(
            date: Long,
            dateSent: Long,
            type: SmsMessageType,
        ): Long =
            when {
                date > 0L -> date
                dateSent > 0L -> dateSent
                type.isOutgoing -> dateSent.coerceAtLeast(date)
                else -> date
            }

        private fun SmsMessage.effectiveTimestamp(): Long =
            when {
                date > 0L -> date
                dateSent > 0L -> dateSent
                else -> 0L
            }

        private fun attachContactNames(messages: List<SmsMessage>): List<SmsMessage> {
            if (!hasContactsPermission() || messages.isEmpty()) return messages
            val namesByAddress = linkedMapOf<String, String?>()
            return messages.map { message ->
                val contactName =
                    namesByAddress.getOrPut(message.address) {
                        resolveContactName(message.address)
                    }
                if (contactName == null) message else message.copy(contactName = contactName)
            }
        }

        private fun resolveContactName(address: String): String? {
            if (address.isBlank() || !hasContactsPermission()) return null
            contactCache[address]?.let { return it }
            val name =
                try {
                    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
                    context.contentResolver.query(
                        uri,
                        arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                } catch (security: SecurityException) {
                    contactsPermissionGranted = false
                    null
                } catch (_: Exception) {
                    null
                }
            contactCache[address] = name
            return name
        }

        private fun Cursor.getStringOrNull(column: String): String? = getColumnIndex(column).takeIf { it >= 0 }?.let { getString(it) }

        private fun Cursor.getLongOrNull(column: String): Long? = getColumnIndex(column).takeIf { it >= 0 }?.let { getLong(it) }

        private fun Cursor.getIntOrNull(column: String): Int? = getColumnIndex(column).takeIf { it >= 0 }?.let { getInt(it) }

        companion object {
            private val FULL_SYNC_URIS =
                listOf(
                    Telephony.Sms.CONTENT_URI,
                    Telephony.Sms.Sent.CONTENT_URI,
                    Telephony.Sms.Outbox.CONTENT_URI,
                )

            private val OUTGOING_FOLDER_URIS =
                listOf(
                    Telephony.Sms.Sent.CONTENT_URI,
                    Telephony.Sms.Outbox.CONTENT_URI,
                )

            private val SMS_PROJECTION =
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.THREAD_ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.DATE_SENT,
                    Telephony.Sms.TYPE,
                    Telephony.Sms.READ,
                    Telephony.Sms.SEEN,
                    Telephony.Sms.STATUS,
                    Telephony.Sms.SUBSCRIPTION_ID,
                    Telephony.Sms.SERVICE_CENTER,
                    Telephony.Sms.PERSON,
                )

            private const val COL_ID = Telephony.Sms._ID
            private const val COL_THREAD_ID = Telephony.Sms.THREAD_ID
            private const val COL_ADDRESS = Telephony.Sms.ADDRESS
            private const val COL_BODY = Telephony.Sms.BODY
            private const val COL_DATE = Telephony.Sms.DATE
            private const val COL_DATE_SENT = Telephony.Sms.DATE_SENT
            private const val COL_TYPE = Telephony.Sms.TYPE
            private const val COL_READ = Telephony.Sms.READ
            private const val COL_SEEN = Telephony.Sms.SEEN
            private const val COL_STATUS = Telephony.Sms.STATUS
            private const val COL_SUBSCRIPTION_ID = Telephony.Sms.SUBSCRIPTION_ID
            private const val COL_SERVICE_CENTER = Telephony.Sms.SERVICE_CENTER
            private const val COL_PERSON = Telephony.Sms.PERSON
        }
    }
