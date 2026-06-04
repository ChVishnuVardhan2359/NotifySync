package com.notifysync.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.notifysync.app.ui.MainViewModel

@Composable
fun SyncedDataScreen(vm: MainViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Notifications", "Calls", "Messages")
    val icons = listOf(Icons.Filled.Notifications, Icons.Filled.Call, Icons.Filled.Message)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { i, t ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text(t) },
                    icon = { Icon(icons[i], contentDescription = t, modifier = Modifier.size(20.dp)) },
                )
            }
        }
        when (tab) {
            0 -> NotificationsTab(vm)
            1 -> CallsTab(vm)
            2 -> MessagesTab(vm)
        }
    }
}

@Composable
private fun NotificationsTab(vm: MainViewModel) {
    val items by vm.syncedNotifications.collectAsState()
    LaunchedEffect(Unit) { vm.loadSyncedNotifications() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { vm.syncNow() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Sync, null, Modifier.size(18.dp)); Text("  Sync current notifications")
        }
        Spacer()
        if (items.isEmpty()) EmptyState("No notifications yet", "Receive a notification or tap Sync.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { n ->
                DataCard(n.appName, shortTime(n.notificationTime), n.title, n.message)
            }
        }
    }
}

@Composable
private fun CallsTab(vm: MainViewModel) {
    val context = LocalContext.current
    val items by vm.calls.collectAsState()
    val syncing by vm.syncingCalls.collectAsState()
    LaunchedEffect(Unit) { vm.loadCalls() }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.syncCalls()
    }
    fun onSync() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) vm.syncCalls() else launcher.launch(Manifest.permission.READ_CALL_LOG)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = ::onSync, enabled = !syncing, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Sync, null, Modifier.size(18.dp))
            Text(if (syncing) "  Syncing calls…" else "  Sync call log")
        }
        Spacer()
        if (items.isEmpty()) EmptyState("No calls synced", "Tap \"Sync call log\" and allow the permission.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { c ->
                DataCard(
                    "${c.callType.replaceFirstChar { it.uppercase() }} • ${c.name ?: c.number}",
                    shortTime(c.callTime),
                    c.number,
                    if (c.durationSeconds > 0) "Duration: ${c.durationSeconds}s" else "",
                )
            }
        }
    }
}

@Composable
private fun MessagesTab(vm: MainViewModel) {
    val context = LocalContext.current
    val items by vm.sms.collectAsState()
    val syncing by vm.syncingSms.collectAsState()
    LaunchedEffect(Unit) { vm.loadSms() }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.syncSms()
    }
    fun onSync() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) vm.syncSms() else launcher.launch(Manifest.permission.READ_SMS)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = ::onSync, enabled = !syncing, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Sync, null, Modifier.size(18.dp))
            Text(if (syncing) "  Syncing messages…" else "  Sync text messages")
        }
        Spacer()
        if (items.isEmpty()) EmptyState("No messages synced", "Tap \"Sync text messages\" and allow the permission.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { m ->
                DataCard(
                    "${if (m.messageType == "sent") "Sent" else "Received"} • ${m.address}",
                    shortTime(m.messageTime),
                    "",
                    m.body,
                )
            }
        }
    }
}

@Composable
private fun DataCard(header: String, time: String, title: String, message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(header, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("  •  $time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            if (title.isNotBlank()) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 2.dp))
            }
            if (message.isNotBlank()) {
                Text(
                    message, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
        )
    }
}

@Composable
private fun Spacer() = androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))

private fun shortTime(iso: String): String = iso.replace('T', ' ').take(16)
