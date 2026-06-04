package com.notifysync.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.notifysync.app.ui.MainViewModel
import com.notifysync.app.ui.screens.AppSelectionScreen
import com.notifysync.app.ui.screens.LoginScreen
import com.notifysync.app.ui.screens.SyncStatusScreen
import com.notifysync.app.ui.screens.SyncedDataScreen
import com.notifysync.app.service.SyncForegroundService
import com.notifysync.app.ui.theme.NotifySyncTheme
import com.notifysync.app.util.BatteryOptimization
import com.notifysync.app.util.NotificationAccess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NotifySyncTheme { NotifyApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifyApp() {
    val vm: MainViewModel = viewModel()
    val nav = rememberNavController()
    val context = LocalContext.current
    val isLoggedIn by vm.isLoggedIn.collectAsState()

    fun callSmsGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    // Track Notification Access + battery exemption + call/SMS perms, re-checking on resume.
    var accessGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    var batteryExempt by remember { mutableStateOf(BatteryOptimization.isIgnoring(context)) }
    var callSmsAllowed by remember { mutableStateOf(callSmsGranted()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessGranted = NotificationAccess.isGranted(context)
                batteryExempt = BatteryOptimization.isIgnoring(context)
                callSmsAllowed = callSmsGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    // Request call-log + SMS so the background service can auto-sync them every ~5 min.
    val callSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { callSmsAllowed = callSmsGranted() }

    // Keep the background sync service running whenever the user is logged in.
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) SyncForegroundService.start(context)
    }

    // Ask for POST_NOTIFICATIONS on Android 13+.
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var title by remember { mutableStateOf("NotifySync") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (title == "Select Apps" || title == "Synced Data") {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (isLoggedIn) "home" else "login",
            modifier = Modifier.padding(padding),
        ) {
            composable("login") {
                title = "NotifySync"
                LoginScreen(vm) {
                    nav.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            }
            composable("home") {
                title = "NotifySync"
                SyncStatusScreen(
                    vm = vm,
                    accessGranted = accessGranted,
                    batteryExempt = batteryExempt,
                    callSmsAllowed = callSmsAllowed,
                    onGrantAccess = { context.startActivity(NotificationAccess.settingsIntent()) },
                    onFixBattery = { context.startActivity(BatteryOptimization.requestIntent(context)) },
                    onGrantCallSms = {
                        callSmsLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALL_LOG,
                                Manifest.permission.READ_SMS,
                                Manifest.permission.READ_CONTACTS,
                            ),
                        )
                    },
                    onPickApps = { nav.navigate("apps") },
                    onViewNotifications = { nav.navigate("synced") },
                    onLogout = {
                        vm.logout()
                        nav.navigate("login") { popUpTo("home") { inclusive = true } }
                    },
                )
            }
            composable("apps") {
                title = "Select Apps"
                AppSelectionScreen(vm)
            }
            composable("synced") {
                title = "Synced Data"
                SyncedDataScreen(vm)
            }
        }
    }
}
