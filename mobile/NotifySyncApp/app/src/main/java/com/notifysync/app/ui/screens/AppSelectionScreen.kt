package com.notifysync.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notifysync.app.ui.MainViewModel

@Composable
fun AppSelectionScreen(vm: MainViewModel) {
    val apps by vm.installedApps.collectAsState()
    val selected by vm.selectedPackages.collectAsState()

    LaunchedEffect(Unit) { vm.loadInstalledApps() }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Select apps to monitor",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        if (apps.isEmpty()) {
            Text("Loading installed apps…", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(apps, key = { it.packageName }) { app ->
                    ListItem(
                        headlineContent = { Text(app.label) },
                        supportingContent = {
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                        },
                        trailingContent = {
                            Checkbox(
                                checked = app.packageName in selected,
                                onCheckedChange = { vm.toggleApp(app.packageName, it) },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Divider()
                }
            }
        }
    }
}
