package com.ghostgramlabs.ghostmask.data.settings

enum class AppLockScope {
    FULL_APP,
    REVEAL_ONLY
}

data class AppSettings(
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = true,
    val lockScope: AppLockScope = AppLockScope.REVEAL_ONLY,
    val lockOnLaunch: Boolean = false,
    val autoLockTimeoutMinutes: Int = 1,
    val rememberRecentEncodedFiles: Boolean = false,
    val defaultSecureView: Boolean = true,
    val defaultScreenshotBlocking: Boolean = true,
    val cleanupTempFiles: Boolean = true,
    val saveToPrivateStorage: Boolean = true,
    val warningDialogsEnabled: Boolean = true,
    val pinConfigured: Boolean = false
)
