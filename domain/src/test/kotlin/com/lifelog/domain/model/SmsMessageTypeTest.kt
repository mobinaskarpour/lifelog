package com.lifelog.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsMessageTypeTest {
    @Test
    fun fromAndroidType_mapsSentAndInbox() {
        assertEquals(SmsMessageType.INBOX, SmsMessageType.fromAndroidType(1))
        assertEquals(SmsMessageType.SENT, SmsMessageType.fromAndroidType(2))
        assertEquals(SmsMessageType.DRAFT, SmsMessageType.fromAndroidType(3))
        assertEquals(SmsMessageType.OUTBOX, SmsMessageType.fromAndroidType(4))
        assertEquals(SmsMessageType.FAILED, SmsMessageType.fromAndroidType(5))
        assertEquals(SmsMessageType.QUEUED, SmsMessageType.fromAndroidType(6))
    }

    @Test
    fun isOutgoing_identifiesOutgoingTypes() {
        assertTrue(SmsMessageType.SENT.isOutgoing)
        assertTrue(SmsMessageType.OUTBOX.isOutgoing)
        assertTrue(SmsMessageType.QUEUED.isOutgoing)
        assertFalse(SmsMessageType.INBOX.isOutgoing)
        assertFalse(SmsMessageType.DRAFT.isOutgoing)
    }
}
