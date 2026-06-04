package com.notifysync.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.notifysync.app.ui.MainViewModel

@Composable
fun SyncStatusScreen(
    vm: MainViewModel,
    accessGranted: Boolean,
    batteryExempt: Boolean,
    callSmsAllowed: Boolean,
    onGrantAccess: () -> Unit,
    onFixBattery: () -> Unit,
    onGrantCallSms: () -> Unit,
    onPickApps: () -> Unit,
    onViewNotifications: () -> Unit,
    onLogout: () -> Unit,
) {
    val displayName by vm.displayName.collectAsState()
    val syncEnabled by vm.syncEnabled.collectAsState()
    val pending by vm.pendingCount.collectAsState()
    val selected by vm.selectedPackages.collectAsState()
    val status by vm.syncStatus.collectAsState()

    // Keep the sync counters live while this screen is open.
    LaunchedEffect(Unit) {
        while (true) {
            vm.loadSyncStatus()
            delay(8_000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Hi, ${displayName.ifBlank { "there" }}", style = MaterialTheme.typography.headlineSmall)
        Text("Notification sync status", style = MaterialTheme.typography.bodyMedium)

        // Live "what's synced to the website" card.
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (status.connected) Icons.Filled.CheckCircle else Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = if (status.connected) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        if (status.connected) "  Syncing to this website" else "  Not connected to website",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    OutlinedButton(onClick = { vm.loadSyncStatus() }) { Text("Refresh") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Stat(status.notifications, "Notifications")
                    Stat(status.calls, "Calls")
                    Stat(status.messages, "Messages")
                }
                if (pending > 0) {
                    Text(
                        "$pending item(s) waiting to upload…",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }

        // Primary action: see what's been captured.
        Button(onClick = onViewNotifications, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  View synced data (notifications, calls, SMS)")
        }

        // Access status card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (accessGranted) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    tint = if (accessGranted) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        if (accessGranted) "Notification access granted" else "Notification access needed",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (accessGranted) "NotifySync can read your notifications."
                        else "Grant access so NotifySync can capture notifications.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (!accessGranted) {
                Button(
                    onClick = onGrantAccess,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                ) { Text("Grant Notification Access") }
            }
        }

        // Battery optimization card
        if (!batteryExempt) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp),
                    )
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Allow background running", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Required so syncing keeps working when the screen is off or the app is closed.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Button(
                    onClick = onFixBattery,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                ) { Text("Disable battery optimization") }
            }
        }

        // Calls + SMS permission card (needed so the background service can auto-sync them)
        if (!callSmsAllowed) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp),
                    )
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Allow calls & SMS sync", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Grant once so your call log and texts sync automatically every ~5 min.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Button(
                    onClick = onGrantCallSms,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                ) { Text("Allow calls & SMS") }
            }
        }

        // Sync toggle card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text("Sync enabled", style = MaterialTheme.typography.titleMedium)
                    Text("Upload captured notifications to the server.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = syncEnabled, onCheckedChange = { vm.setSyncEnabled(it) })
            }
        }

        // App selection card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Apps, contentDescription = null, modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text("Monitored apps", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (selected.isEmpty()) "All apps (tap Choose to narrow down)"
                        else "${selected.size} app(s) selected",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(onClick = onPickApps) { Text("Choose") }
            }
        }

        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Logout") }
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
