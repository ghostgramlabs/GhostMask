package com.ghostgramlabs.ghostmask.domain.reveal

import com.ghostgramlabs.ghostmask.data.reveal.EncodedFileFingerprintStore
import com.ghostgramlabs.ghostmask.domain.model.GhostMeta
import com.ghostgramlabs.ghostmask.storage.TempFileManager

class RevealPolicyEnforcer(
    private val fingerprintStore: EncodedFileFingerprintStore,
    private val tempFileManager: TempFileManager,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun validateBeforeReveal(meta: GhostMeta, fingerprint: String): RevealPrecheckResult {
        val expiry = meta.expiryEpochMs
        if (expiry != null && nowProvider() > expiry) {
            tempFileManager.clearSensitiveCache()
            return RevealPrecheckResult.Blocked("This secret has expired.")
        }

        if (meta.flags.oneTimeReveal && fingerprintStore.hasBeenRevealed(fingerprint)) {
            tempFileManager.clearSensitiveCache()
            return RevealPrecheckResult.Blocked("This secret was already opened on this device.")
        }

        return RevealPrecheckResult.Allowed
    }

    suspend fun onRevealCommitted(meta: GhostMeta, fingerprint: String) {
        if (meta.flags.oneTimeReveal) {
            fingerprintStore.markRevealed(fingerprint)
        }
    }

    suspend fun clearSensitiveArtifacts() {
        tempFileManager.clearSensitiveCache()
    }
}

sealed interface RevealPrecheckResult {
    data object Allowed : RevealPrecheckResult
    data class Blocked(val message: String) : RevealPrecheckResult
}
