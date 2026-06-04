package com.notifysync.app.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notifysync.app.data.api.CallDto
import com.notifysync.app.data.api.NotificationDto
import com.notifysync.app.data.api.SmsDto
import com.notifysync.app.data.repository.DeviceDataRepository
import com.notifysync.app.data.local.TokenStore
import com.notifysync.app.data.repository.AuthRepository
import com.notifysync.app.data.repository.SyncRepository
import com.notifysync.app.worker.UploadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledApp(val packageName: String, val label: String)

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenStore = TokenStore(app)
    private val authRepository = AuthRepository(app, tokenStore)
    private val syncRepository = SyncRepository(app, tokenStore)
    private val deviceDataRepository = DeviceDataRepository(app, tokenStore)

    val isLoggedIn: StateFlow<Boolean> =
        tokenStore.isLoggedInFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val displayName: StateFlow<String> =
        tokenStore.displayNameFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val syncEnabled: StateFlow<Boolean> =
        tokenStore.syncEnabledFlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val selectedPackages: StateFlow<Set<String>> =
        tokenStore.selectedPackagesFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val pendingCount: StateFlow<Int> =
        syncRepository.pendingCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val serverUrl: StateFlow<String> =
        tokenStore.serverUrlFlow
            .map { it.ifBlank { com.notifysync.app.BuildConfig.API_BASE_URL } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, com.notifysync.app.BuildConfig.API_BASE_URL)

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _syncedNotifications = MutableStateFlow<List<NotificationDto>>(emptyList())
    val syncedNotifications: StateFlow<List<NotificationDto>> = _syncedNotifications.asStateFlow()

    // Live "what's synced to the website" status, shown on the home screen.
    data class SyncStatus(
        val connected: Boolean = false,
        val notifications: Int = 0,
        val calls: Int = 0,
        val messages: Int = 0,
        val loading: Boolean = false,
    )

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun loadSyncStatus() {
        viewModelScope.launch {
            _syncStatus.value = _syncStatus.value.copy(loading = true)
            val notifs = syncRepository.notificationTotal()
            val calls = deviceDataRepository.callTotal()
            val msgs = deviceDataRepository.smsTotal()
            _syncStatus.value = SyncStatus(
                connected = notifs != null,
                notifications = notifs ?: 0,
                calls = calls ?: 0,
                messages = msgs ?: 0,
                loading = false,
            )
        }
    }

    private val _loadingNotifications = MutableStateFlow(false)
    val loadingNotifications: StateFlow<Boolean> = _loadingNotifications.asStateFlow()

    fun loadSyncedNotifications() {
        viewModelScope.launch {
            _loadingNotifications.value = true
            _syncedNotifications.value = syncRepository.recentNotifications()
            _loadingNotifications.value = false
        }
    }

    /** Pull current notifications from the status bar, upload them, then refresh the list. */
    fun syncNow(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            com.notifysync.app.service.NotifyListenerService.instance?.captureActiveNotifications()
            syncRepository.uploadPending()
            _syncedNotifications.value = syncRepository.recentNotifications()
            onDone()
        }
    }

    // ---- Calls ----
    private val _calls = MutableStateFlow<List<CallDto>>(emptyList())
    val calls: StateFlow<List<CallDto>> = _calls.asStateFlow()
    private val _syncingCalls = MutableStateFlow(false)
    val syncingCalls: StateFlow<Boolean> = _syncingCalls.asStateFlow()

    fun loadCalls() {
        viewModelScope.launch { _calls.value = deviceDataRepository.recentCalls() }
    }

    fun syncCalls(onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _syncingCalls.value = true
            val inserted = deviceDataRepository.syncCalls()
            _calls.value = deviceDataRepository.recentCalls()
            _syncingCalls.value = false
            onResult(inserted)
        }
    }

    // ---- SMS ----
    private val _sms = MutableStateFlow<List<SmsDto>>(emptyList())
    val sms: StateFlow<List<SmsDto>> = _sms.asStateFlow()
    private val _syncingSms = MutableStateFlow(false)
    val syncingSms: StateFlow<Boolean> = _syncingSms.asStateFlow()

    fun loadSms() {
        viewModelScope.launch { _sms.value = deviceDataRepository.recentSms() }
    }

    fun syncSms(onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _syncingSms.value = true
            val inserted = deviceDataRepository.syncSms()
            _sms.value = deviceDataRepository.recentSms()
            _syncingSms.value = false
            onResult(inserted)
        }
    }

    fun login(serverUrl: String, email: String, password: String, onSuccess: () -> Unit) {
        submit(serverUrl, onSuccess) { authRepository.login(email, password) }
    }

    fun register(
        serverUrl: String,
        email: String,
        password: String,
        first: String,
        last: String,
        onSuccess: () -> Unit,
    ) {
        submit(serverUrl, onSuccess) { authRepository.register(email, password, first, last) }
    }

    private fun submit(serverUrl: String, onSuccess: () -> Unit, action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _loginState.value = LoginUiState(loading = true)
            tokenStore.setServerUrl(serverUrl) // apply chosen server before the call
            val result = action()
            _loginState.value = if (result.isSuccess) {
                onSuccess()
                LoginUiState()
            } else {
                LoginUiState(error = result.exceptionOrNull()?.message ?: "Something went wrong")
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            tokenStore.setSyncEnabled(enabled)
            if (enabled) UploadWorker.enqueue(getApplication())
        }
    }

    fun toggleApp(packageName: String, selected: Boolean) {
        viewModelScope.launch {
            val current = tokenStore.selectedPackages().toMutableSet()
            if (selected) current.add(packageName) else current.remove(packageName)
            tokenStore.setSelectedPackages(current)
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                pm.getInstalledApplications(0)
                    .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || pm.getLaunchIntentForPackage(it.packageName) != null }
                    .map { InstalledApp(it.packageName, pm.getApplicationLabel(it).toString()) }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }
        }
    }
}
