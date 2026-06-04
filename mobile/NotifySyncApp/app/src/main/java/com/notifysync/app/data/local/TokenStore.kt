package com.notifysync.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "notifysync_prefs")

/**
 * Wraps Jetpack DataStore for auth token, device identity, the set of selected
 * app packages to capture, and the sync on/off flag.
 */
class TokenStore(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val DEVICE_IDENTIFIER = stringPreferencesKey("device_identifier")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val SELECTED_PACKAGES = stringSetPreferencesKey("selected_packages")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        val SERVER_URL = stringPreferencesKey("server_url")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { !it[Keys.TOKEN].isNullOrBlank() }
    val displayNameFlow: Flow<String> = context.dataStore.data.map { it[Keys.DISPLAY_NAME] ?: "" }
    val selectedPackagesFlow: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.SELECTED_PACKAGES] ?: emptySet() }
    val syncEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.SYNC_ENABLED] ?: true }
    val deviceIdFlow: Flow<String?> = context.dataStore.data.map { it[Keys.DEVICE_ID] }
    val serverUrlFlow: Flow<String> = context.dataStore.data.map { it[Keys.SERVER_URL] ?: "" }

    suspend fun serverUrl(): String? = context.dataStore.data.first()[Keys.SERVER_URL]

    /** Stores the API base URL, normalising it to a scheme-qualified value ending in '/'. */
    suspend fun setServerUrl(url: String) {
        var normalized = url.trim()
        if (normalized.isEmpty()) return
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        if (!normalized.endsWith("/")) normalized += "/"
        context.dataStore.edit { it[Keys.SERVER_URL] = normalized }
    }

    suspend fun token(): String? = context.dataStore.data.first()[Keys.TOKEN]
    suspend fun deviceId(): String? = context.dataStore.data.first()[Keys.DEVICE_ID]
    suspend fun deviceIdentifier(): String? = context.dataStore.data.first()[Keys.DEVICE_IDENTIFIER]
    suspend fun selectedPackages(): Set<String> =
        context.dataStore.data.first()[Keys.SELECTED_PACKAGES] ?: emptySet()
    suspend fun syncEnabled(): Boolean = context.dataStore.data.first()[Keys.SYNC_ENABLED] ?: true

    suspend fun saveAuth(token: String, email: String, displayName: String) {
        context.dataStore.edit {
            it[Keys.TOKEN] = token
            it[Keys.EMAIL] = email
            it[Keys.DISPLAY_NAME] = displayName
        }
    }

    suspend fun saveDevice(id: String, identifier: String, name: String) {
        context.dataStore.edit {
            it[Keys.DEVICE_ID] = id
            it[Keys.DEVICE_IDENTIFIER] = identifier
            it[Keys.DEVICE_NAME] = name
        }
    }

    suspend fun setSelectedPackages(packages: Set<String>) {
        context.dataStore.edit { it[Keys.SELECTED_PACKAGES] = packages }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SYNC_ENABLED] = enabled }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
