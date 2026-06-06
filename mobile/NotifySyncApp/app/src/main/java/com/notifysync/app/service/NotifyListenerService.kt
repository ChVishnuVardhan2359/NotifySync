package com.notifysync.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.notifysync.app.data.local.PendingNotification
import com.notifysync.app.data.local.TokenStore
import com.notifysync.app.data.repository.SyncRepository
import com.notifysync.app.worker.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Listens to system notifications. For packages the user selected (or all apps when none are
 * selected), it extracts message content and persists it to the offline queue, then triggers
 * upload. Handles chat apps (WhatsApp, Telegram, Messenger…) that use MessagingStyle so each
 * message is captured with its sender, and de-duplicates repeated notification updates.
 */
class NotifyListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var tokenStore: TokenStore
    private lateinit var repo: SyncRepository

    // Small LRU of recently-captured keys so chat notifications (which re-post their history
    // on every new message) don't create duplicates.
    private val recentKeys = object : LinkedHashMap<String, Boolean>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) = size > 500
    }

    private data class Captured(val title: String, val text: String, val time: Long)

    override fun onCreate() {
        super.onCreate()
        tokenStore = TokenStore(applicationContext)
        repo = SyncRepository(applicationContext, tokenStore)
        instance = this
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handle(sbn)
    }

    private fun handle(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        if (packageName == applicationContext.packageName) return // ignore our own

        val captures = extract(sbn)
        if (captures.isEmpty()) return
        val appName = resolveAppName(packageName)

        scope.launch {
            if (!tokenStore.syncEnabled()) return@launch
            val selected = tokenStore.selectedPackages()
            // Empty selection => monitor ALL apps (so it works out of the box).
            if (selected.isNotEmpty() && packageName !in selected) return@launch

            var queuedAny = false
            for (c in captures) {
                val key = "$packageName|${c.title}|${c.text}|${c.time}"
                if (!markNew(key)) continue
                repo.enqueue(
                    PendingNotification(
                        eventKey = key,
                        appName = appName,
                        packageName = packageName,
                        title = c.title,
                        message = c.text,
                        notificationTime = Instant.ofEpochMilli(c.time).toString(),
                    ),
                )
                queuedAny = true
            }
            if (queuedAny) UploadWorker.enqueue(applicationContext)
        }
    }

    /** Pull message(s) out of a notification, handling chat-style and bundled formats. */
    private fun extract(sbn: StatusBarNotification): List<Captured> {
        val n = sbn.notification ?: return emptyList()
        // Skip the "group summary" (e.g. "5 messages from 3 chats") — the per-chat ones carry content.
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return emptyList()

        val extras = n.extras ?: return emptyList()
        val appTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()

        // 1) MessagingStyle — WhatsApp, Telegram, Messenger, Signal, etc.
        val style = runCatching {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        }.getOrNull()
        if (style != null && style.messages.isNotEmpty()) {
            val conversation = style.conversationTitle?.toString()?.ifBlank { null }
            return style.messages.mapNotNull { m ->
                val text = m.text?.toString().orEmpty()
                if (text.isBlank()) return@mapNotNull null
                val sender = m.person?.name?.toString()?.ifBlank { null }
                // Group chats => "Group: Sender"; 1:1 => just the sender.
                val title = when {
                    conversation != null && sender != null && conversation != sender -> "$conversation: $sender"
                    sender != null -> sender
                    conversation != null -> conversation
                    else -> appTitle
                }
                Captured(title, text, m.timestamp)
            }
        }

        // 2) InboxStyle — multiple lines bundled into one notification.
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (lines != null && lines.isNotEmpty()) {
            return lines.mapNotNull {
                val text = it?.toString().orEmpty()
                if (text.isBlank()) null else Captured(appTitle, text, sbn.postTime)
            }
        }

        // 3) BigText, then plain text.
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.ifBlank { null }
        val text = big ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (appTitle.isBlank() && text.isBlank()) return emptyList()
        return listOf(Captured(appTitle, text, sbn.postTime))
    }

    private fun markNew(key: String): Boolean = synchronized(recentKeys) {
        if (recentKeys.containsKey(key)) false else { recentKeys[key] = true; true }
    }

    /** Grab everything currently in the status bar and queue it (used by "Sync Now"). */
    fun captureActiveNotifications() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        active.forEach { handle(it) }
    }

    private fun resolveAppName(packageName: String): String = runCatching {
        val pm = applicationContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
        scope.cancel()
    }

    companion object {
        @Volatile
        var instance: NotifyListenerService? = null
            private set
    }
}
