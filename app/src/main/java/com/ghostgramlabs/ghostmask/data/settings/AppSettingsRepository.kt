package com.ghostgramlabs.ghostmask.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ghostgramlabs.ghostmask.security.PinSecurity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "ghostmask_app_settings")

class AppSettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.appSettingsDataStore.data.map { prefs ->
        AppSettings(
            appLockEnabled = prefs[KEY_APP_LOCK_ENABLED] ?: false,
            biometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: true,
            lockScope = AppLockScope.valueOf(prefs[KEY_LOCK_SCOPE] ?: AppLockScope.REVEAL_ONLY.name),
            lockOnLaunch = prefs[KEY_LOCK_ON_LAUNCH] ?: false,
            autoLockTimeoutMinutes = prefs[KEY_AUTO_LOCK_TIMEOUT_MINUTES] ?: 1,
            rememberRecentEncodedFiles = prefs[KEY_REMEMBER_RECENT_FILES] ?: false,
            defaultSecureView = prefs[KEY_DEFAULT_SECURE_VIEW] ?: true,
            defaultScreenshotBlocking = prefs[KEY_DEFAULT_SCREENSHOT_BLOCKING] ?: true,
            cleanupTempFiles = prefs[KEY_CLEANUP_TEMP_FILES] ?: true,
            saveToPrivateStorage = prefs[KEY_SAVE_TO_PRIVATE_STORAGE] ?: true,
            warningDialogsEnabled = prefs[KEY_WARNING_DIALOGS_ENABLED] ?: true,
            pinConfigured = !prefs[KEY_PIN_HASH].isNullOrBlank() && !prefs[KEY_PIN_SALT].isNullOrBlank()
        )
    }

    suspend fun updateAppLockEnabled(enabled: Boolean) = updateBoolean(KEY_APP_LOCK_ENABLED, enabled)
    suspend fun updateBiometricEnabled(enabled: Boolean) = updateBoolean(KEY_BIOMETRIC_ENABLED, enabled)
    suspend fun updateLockOnLaunch(enabled: Boolean) = updateBoolean(KEY_LOCK_ON_LAUNCH, enabled)
    suspend fun updateRememberRecentEncodedFiles(enabled: Boolean) = updateBoolean(KEY_REMEMBER_RECENT_FILES, enabled)
    suspend fun updateDefaultSecureView(enabled: Boolean) = updateBoolean(KEY_DEFAULT_SECURE_VIEW, enabled)
    suspend fun updateDefaultScreenshotBlocking(enabled: Boolean) = updateBoolean(KEY_DEFAULT_SCREENSHOT_BLOCKING, enabled)
    suspend fun updateCleanupTempFiles(enabled: Boolean) = updateBoolean(KEY_CLEANUP_TEMP_FILES, enabled)
    suspend fun updateSaveToPrivateStorage(enabled: Boolean) = updateBoolean(KEY_SAVE_TO_PRIVATE_STORAGE, enabled)
    suspend fun updateWarningDialogsEnabled(enabled: Boolean) = updateBoolean(KEY_WARNING_DIALOGS_ENABLED, enabled)

    suspend fun updateLockScope(scope: AppLockScope) {
        context.appSettingsDataStore.edit { it[KEY_LOCK_SCOPE] = scope.name }
    }

    suspend fun updateAutoLockTimeoutMinutes(minutes: Int) {
        context.appSettingsDataStore.edit { it[KEY_AUTO_LOCK_TIMEOUT_MINUTES] = minutes.coerceAtLeast(0) }
    }

    suspend fun savePin(pin: String) {
        val salt = PinSecurity.generateSaltBase64()
        val hash = PinSecurity.hashPin(pin, salt)
        context.appSettingsDataStore.edit {
            it[KEY_PIN_SALT] = salt
            it[KEY_PIN_HASH] = hash
        }
    }

    suspend fun clearPin() {
        context.appSettingsDataStore.edit {
            it.remove(KEY_PIN_SALT)
            it.remove(KEY_PIN_HASH)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.appSettingsDataStore.data.first()
        val salt = prefs[KEY_PIN_SALT] ?: return false
        val hash = prefs[KEY_PIN_HASH] ?: return false
        return PinSecurity.verifyPin(pin, salt, hash)
    }

    private suspend fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.appSettingsDataStore.edit { it[key] = value }
    }

    companion object {
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_LOCK_SCOPE = stringPreferencesKey("lock_scope")
        private val KEY_LOCK_ON_LAUNCH = booleanPreferencesKey("lock_on_launch")
        private val KEY_AUTO_LOCK_TIMEOUT_MINUTES = intPreferencesKey("auto_lock_timeout_minutes")
        private val KEY_REMEMBER_RECENT_FILES = booleanPreferencesKey("remember_recent_files")
        private val KEY_DEFAULT_SECURE_VIEW = booleanPreferencesKey("default_secure_view")
        private val KEY_DEFAULT_SCREENSHOT_BLOCKING = booleanPreferencesKey("default_screenshot_blocking")
        private val KEY_CLEANUP_TEMP_FILES = booleanPreferencesKey("cleanup_temp_files")
        private val KEY_SAVE_TO_PRIVATE_STORAGE = booleanPreferencesKey("save_to_private_storage")
        private val KEY_WARNING_DIALOGS_ENABLED = booleanPreferencesKey("warning_dialogs_enabled")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
    }
}
