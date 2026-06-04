package com.notifysync.app.data.local

import android.content.Context
import android.provider.CallLog
import android.provider.Telephony
import com.notifysync.app.data.api.CallItem
import com.notifysync.app.data.api.SmsItem
import java.time.Instant

/** Reads call log + SMS from the system content providers (requires the relevant permissions). */
object DeviceDataReader {

    private fun iso(millis: Long): String = Instant.ofEpochMilli(millis).toString()

    fun readCalls(context: Context, limit: Int = 500): List<CallItem> {
        val items = mutableListOf<CallItem>()
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
        )
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, projection, null, null,
            "${CallLog.Calls.DATE} DESC",
        )?.use { c ->
            val numIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
            val durIdx = c.getColumnIndex(CallLog.Calls.DURATION)
            while (c.moveToNext() && items.size < limit) {
                val number = c.getString(numIdx) ?: "unknown"
                val name = c.getString(nameIdx)
                val type = callType(c.getInt(typeIdx))
                val date = c.getLong(dateIdx)
                val duration = c.getInt(durIdx)
                items.add(
                    CallItem(
                        sourceKey = "${date}_${number}_${type}",
                        number = number,
                        name = name,
                        callType = type,
                        callTime = iso(date),
                        durationSeconds = duration,
                    ),
                )
            }
        }
        return items
    }

    fun readSms(context: Context, limit: Int = 500): List<SmsItem> {
        val items = mutableListOf<SmsItem>()
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
        )
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, projection, null, null,
            "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            val addrIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndex(Telephony.Sms.TYPE)
            while (c.moveToNext() && items.size < limit) {
                val address = c.getString(addrIdx) ?: "unknown"
                val body = c.getString(bodyIdx) ?: ""
                val date = c.getLong(dateIdx)
                val type = if (c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_SENT) "sent" else "inbox"
                items.add(
                    SmsItem(
                        sourceKey = "${date}_${address}_${type}",
                        address = address,
                        body = body,
                        messageType = type,
                        messageTime = iso(date),
                    ),
                )
            }
        }
        return items
    }

    private fun callType(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "incoming"
        CallLog.Calls.OUTGOING_TYPE -> "outgoing"
        CallLog.Calls.MISSED_TYPE -> "missed"
        CallLog.Calls.REJECTED_TYPE -> "rejected"
        CallLog.Calls.VOICEMAIL_TYPE -> "voicemail"
        else -> "unknown"
    }
}
