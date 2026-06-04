package com.notifysync.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.notifysync.app.data.api.SmsItem
import com.notifysync.app.data.local.TokenStore
import com.notifysync.app.data.repository.DeviceDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/** Real-time: uploads incoming SMS the moment they arrive (requires RECEIVE_SMS). */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // Multipart SMS arrive as several parts — concatenate the bodies.
        val address = messages.first().originatingAddress ?: "unknown"
        val timestamp = messages.first().timestampMillis
        val body = messages.joinToString("") { it.messageBody ?: "" }

        val item = SmsItem(
            sourceKey = "${timestamp}_${address}_inbox",
            address = address,
            body = body,
            messageType = "inbox",
            messageTime = Instant.ofEpochMilli(timestamp).toString(),
        )

        val pending = goAsync()
        val repo = DeviceDataRepository(context.applicationContext, TokenStore(context.applicationContext))
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { repo.uploadSmsItems(listOf(item)) }
            pending.finish()
        }
    }
}
