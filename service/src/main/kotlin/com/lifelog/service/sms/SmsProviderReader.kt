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

        fun readAllMessages(): SmsReadResult {
            if (!hasReadPermission()) {
                return SmsReadResult.Failure(
                    SmsAccessStatus.PERMISSION_DENIED,
                    "SMS permission is required to read messages from this device.",
                )
            }

            val messages = mutableListOf<SmsMessage>()

            return try {
                val cursor =
                    context.contentResolver.query(
                        Telephony.Sms.CONTENT_URI,
                        SMS_PROJECTION,
                        null,
                        null,
                        "${Telephony.Sms.DATE} DESC",
                    ) ?: return SmsReadResult.Failure(
                        SmsAccessStatus.PROVIDER_UNAVAILABLE,
                        "SMS content provider is not available on this device.",
                    )

                cursor.use {
                    while (it.moveToNext()) {
                        mapCursorToMessage(it)?.let { message -> messages.add(message) }
                    }
                }

                val finalStats =
                    SmsSyncStats(
                        providerInboxCount = messages.count { it.type == SmsMessageType.INBOX },
                        providerSentCount = messages.count { it.type == SmsMessageType.SENT },
                        providerOutboxCount = messages.count { it.type == SmsMessageType.OUTBOX },
                        providerDraftCount = messages.count { it.type == SmsMessageType.DRAFT },
                    )

                Timber.d(
                    "SMS provider read: inbox=${finalStats.providerInboxCount} " +
                        "sent=${finalStats.providerSentCount} outbox=${finalStats.providerOutboxCount} " +
                        "draft=${finalStats.providerDraftCount}",
                )

                SmsReadResult.Success(messages, finalStats)
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

        private fun mapCursorToMessage(cursor: Cursor): SmsMessage? {
            val providerId = cursor.getLongOrNull(COL_ID) ?: return null
            val threadId = cursor.getLongOrNull(COL_THREAD_ID) ?: 0L
            val address = cursor.getStringOrNull(COL_ADDRESS).orEmpty().ifBlank { "Unknown" }
            val body = cursor.getStringOrNull(COL_BODY).orEmpty()
            val date = cursor.getLongOrNull(COL_DATE) ?: 0L
            val dateSent = cursor.getLongOrNull(COL_DATE_SENT) ?: date
            val type = SmsMessageType.fromAndroidType(cursor.getIntOrNull(COL_TYPE) ?: 0)
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
                contactName = resolveContactName(address),
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

        private fun resolveContactName(address: String): String? {
            if (address.isBlank()) return null
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
