package com.ghostgramlabs.ghostmask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgramlabs.ghostmask.data.settings.AppSettings
import com.ghostgramlabs.ghostmask.data.settings.AppSettingsRepository
import com.ghostgramlabs.ghostmask.storage.TempFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppSecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppSettingsRepository(application)
    private val tempFileManager = TempFileManager(application)

    private val _uiState = MutableStateFlow(AppSecurityUiState())
    val uiState: StateFlow<AppSecurityUiState> = _uiState.asStateFlow()

    private var backgroundedAtEpochMs: Long? = null
    private var unlockPending = false
    private var lastUnlockEpochMs: Long? = null

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                if (_uiState.value.settings == null) {
                    unlockPending = settings.appLockEnabled && settings.lockOnLaunch
                } else if (!settings.appLockEnabled) {
                    unlockPending = false
                }
                _uiState.update {
                    it.copy(
                        settings = settings,
                        unlockPending = if (settings.appLockEnabled) unlockPending else false,
                        pinConfigured = settings.pinConfigured
                    )
                }
            }
        }
    }

    fun onRouteChanged(route: String?) {
        _uiState.update { it.copy(currentRoute = route) }
        if (unlockPending) {
            _uiState.update { it.copy(unlockPending = true, errorMessage = null) }
        }
    }

    fun onAppBackgrounded() {
        backgroundedAtEpochMs = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val settings = _uiState.value.settings ?: return
        if (!settings.appLockEnabled) return
        val shouldLock = settings.lockOnLaunch || hasTimeoutElapsed(settings)
        if (shouldLock) {
            unlockPending = true
            _uiState.update { it.copy(unlockPending = true, errorMessage = null) }
        }
        backgroundedAtEpochMs = null
    }

    fun onBiometricUnlockSucceeded() {
        lastUnlockEpochMs = System.currentTimeMillis()
        unlockPending = false
        _uiState.update { it.copy(unlockPending = false, errorMessage = null) }
    }

    fun onBiometricUnlockFailed(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun unlockWithPin(pin: String) {
        viewModelScope.launch {
            if (repository.verifyPin(pin)) {
                lastUnlockEpochMs = System.currentTimeMillis()
                unlockPending = false
                _uiState.update { it.copy(unlockPending = false, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = "Incorrect PIN.") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun lockNow() {
        val settings = _uiState.value.settings ?: return
        if (!settings.appLockEnabled) return
        unlockPending = true
        _uiState.update { it.copy(unlockPending = true) }
    }

    fun updateAppLockEnabled(enabled: Boolean) = launchUpdate { repository.updateAppLockEnabled(enabled) }
    fun updateBiometricEnabled(enabled: Boolean) = launchUpdate {
        repository.updateBiometricEnabled(enabled)
        if (enabled) {
            repository.updateAppLockEnabled(true)
        }
    }
    fun updateLockOnLaunch(enabled: Boolean) = launchUpdate { repository.updateLockOnLaunch(enabled) }
    fun updateAutoLockTimeoutMinutes(minutes: Int) = launchUpdate { repository.updateAutoLockTimeoutMinutes(minutes) }
    fun updateRememberRecentFiles(enabled: Boolean) = launchUpdate { repository.updateRememberRecentEncodedFiles(enabled) }
    fun updateDefaultSecureView(enabled: Boolean) = launchUpdate { repository.updateDefaultSecureView(enabled) }
    fun updateDefaultScreenshotBlocking(enabled: Boolean) = launchUpdate { repository.updateDefaultScreenshotBlocking(enabled) }
    fun updateCleanupTempFiles(enabled: Boolean) = launchUpdate { repository.updateCleanupTempFiles(enabled) }
    fun updateSaveToPrivateStorage(enabled: Boolean) = launchUpdate { repository.updateSaveToPrivateStorage(enabled) }
    fun updateWarningDialogs(enabled: Boolean) = launchUpdate { repository.updateWarningDialogsEnabled(enabled) }

    fun savePin(pin: String) {
        viewModelScope.launch {
            repository.savePin(pin)
            repository.updateAppLockEnabled(true)
            _uiState.update { it.copy(errorMessage = "PIN updated.") }
        }
    }

    fun clearPin() {
        viewModelScope.launch {
            repository.clearPin()
            _uiState.update { it.copy(errorMessage = "PIN removed.") }
        }
    }

    fun clearSensitiveCache() {
        viewModelScope.launch {
            tempFileManager.clearSensitiveCache()
            _uiState.update { it.copy(errorMessage = "Sensitive cache cleared.") }
        }
    }

    fun shouldRequireUnlock(route: String?): Boolean {
        val settings = _uiState.value.settings ?: return false
        return settings.appLockEnabled && unlockPending
    }

    private fun hasTimeoutElapsed(settings: AppSettings): Boolean {
        val lastBackground = backgroundedAtEpochMs ?: return false
        val elapsedMs = System.currentTimeMillis() - lastBackground
        val timeoutMs = settings.autoLockTimeoutMinutes * 60_000L
        return settings.autoLockTimeoutMinutes == 0 || elapsedMs >= timeoutMs
    }

    private fun launchUpdate(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

data class AppSecurityUiState(
    val settings: AppSettings? = null,
    val currentRoute: String? = null,
    val unlockPending: Boolean = false,
    val pinConfigured: Boolean = false,
    val errorMessage: String? = null
)
