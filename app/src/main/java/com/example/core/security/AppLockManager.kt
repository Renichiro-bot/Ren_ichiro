package com.example.core.security

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class AppLockManager(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) {

    private val keyguardManager: KeyguardManager? =
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("pref_app_lock_enabled")

    val isAppLockEnabledStream: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_APP_LOCK_ENABLED] ?: false
    }

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    fun isDeviceSecure(): Boolean {
        return keyguardManager?.isDeviceSecure ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_LOCK_ENABLED] = enabled
        }
        if (!enabled) {
            _isSessionUnlocked.value = true
        }
    }

    fun markSessionUnlocked() {
        _isSessionUnlocked.value = true
    }

    fun lockSession() {
        _isSessionUnlocked.value = false
    }

    fun createUnlockIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            keyguardManager?.createConfirmDeviceCredentialIntent(
                "Unlock Vellum",
                "Authenticate using your device security to access your notes."
            )
        } else {
            null
        }
    }
}
