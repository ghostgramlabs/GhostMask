package com.ghostgramlabs.ghostmask.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgramlabs.ghostmask.data.reveal.EncodedFileFingerprintStore
import com.ghostgramlabs.ghostmask.data.reveal.RecentEncodedFilesStore
import com.ghostgramlabs.ghostmask.data.settings.AppSettingsRepository
import com.ghostgramlabs.ghostmask.domain.model.GhostMeta
import com.ghostgramlabs.ghostmask.domain.reveal.RevealPolicyEnforcer
import com.ghostgramlabs.ghostmask.domain.reveal.RevealPrecheckResult
import com.ghostgramlabs.ghostmask.storage.TempFileManager
import com.ghostgramlabs.ghostmask.stego.ImageCompressionHelper
import com.ghostgramlabs.ghostmask.stego.InvalidPayloadException
import com.ghostgramlabs.ghostmask.stego.PayloadParser
import com.ghostgramlabs.ghostmask.stego.StegoDecoder
import com.ghostgramlabs.ghostmask.stego.WrongPasswordException
import com.ghostgramlabs.ghostmask.util.BitmapUtils
import com.ghostgramlabs.ghostmask.util.FileSaveManager
import java.security.MessageDigest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RevealSecretsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = AppSettingsRepository(application)
    private val recentFilesStore = RecentEncodedFilesStore(application)
    private val tempFileManager = TempFileManager(application)
    private val policyEnforcer = RevealPolicyEnforcer(
        fingerprintStore = EncodedFileFingerprintStore(application),
        tempFileManager = tempFileManager
    )

    private val _uiState = MutableStateFlow(RevealSecretsUiState())
    val uiState: StateFlow<RevealSecretsUiState> = _uiState.asStateFlow()

    private var encodedBitmap: Bitmap? = null
    private var revealedImageBitmap: Bitmap? = null
    private var revealedImageBytes: ByteArray? = null
    private var pendingReveal: PendingReveal? = null
    private var timerJob: Job? = null
    private var pendingDeleteAfterSessionUri: Uri? = null

    fun getRevealedImageBitmap(): Bitmap? = revealedImageBitmap

    fun setEncodedImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    decodingResult = null,
                    pendingDeleteConfirmation = false
                )
            }
            try {
                try {
                    getApplication<Application>().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Some providers do not support persistable write permission.
                }
                val bitmap = BitmapUtils.loadBitmap(getApplication(), uri, maxDimension = 0)
                if (bitmap == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load image.") }
                    return@launch
                }
                encodedBitmap = bitmap
                clearRevealSession()
                _uiState.update { it.copy(encodedImageUri = uri, isLoading = false, password = "") }
                val settings = settingsRepository.settings.first()
                if (settings.rememberRecentEncodedFiles) {
                    recentFilesStore.remember(uri)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error loading image: ${e.message}") }
            }
        }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun decode() {
        val bitmap = encodedBitmap
        val password = _uiState.value.password

        if (bitmap == null) {
            _uiState.update { it.copy(errorMessage = "Please select an encoded image.") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Password is required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDecoding = true, errorMessage = null, pendingBiometricPrompt = false) }
            try {
                clearRevealSession()
                val extracted = StegoDecoder.decodeWithAutoDetect(bitmap)
                val decoded = PayloadParser.parse(extracted.payload, password)
                val fingerprint = fingerprint(extracted.payload)

                when (val precheck = policyEnforcer.validateBeforeReveal(decoded.payload.meta, fingerprint)) {
                    is RevealPrecheckResult.Blocked -> {
                        _uiState.update { it.copy(isDecoding = false, errorMessage = precheck.message) }
                    }
                    RevealPrecheckResult.Allowed -> {
                        pendingReveal = PendingReveal(
                            decoded = decoded,
                            fingerprint = fingerprint,
                            encodedUri = _uiState.value.encodedImageUri
                        )
                        if (decoded.payload.meta.flags.requireBiometric) {
                            _uiState.update { it.copy(isDecoding = false, pendingBiometricPrompt = true) }
                        } else {
                            commitReveal()
                        }
                    }
                }
            } catch (e: WrongPasswordException) {
                _uiState.update { it.copy(isDecoding = false, errorMessage = "Wrong password or corrupted hidden data.") }
            } catch (e: InvalidPayloadException) {
                _uiState.update { it.copy(isDecoding = false, errorMessage = e.message ?: "This image does not contain a GhostMask secret.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDecoding = false, errorMessage = "Decoding failed: ${e.message}") }
            }
        }
    }

    fun onBiometricPromptShown() {
        _uiState.update { it.copy(pendingBiometricPrompt = false) }
    }

    fun onBiometricResult(success: Boolean, message: String? = null) {
        viewModelScope.launch {
            if (success) {
                commitReveal()
            } else {
                pendingReveal = null
                _uiState.update { it.copy(errorMessage = message ?: "Authentication failed.", isDecoding = false) }
            }
        }
    }

    private suspend fun commitReveal() {
        val reveal = pendingReveal ?: return
        val meta = reveal.decoded.payload.meta
        val imageBytes = reveal.decoded.imageBytes

        var bitmap: Bitmap? = null
        if (imageBytes != null) {
            bitmap = ImageCompressionHelper.decompressImage(imageBytes)
            if (bitmap == null) {
                _uiState.update {
                    it.copy(
                        isDecoding = false,
                        errorMessage = "Hidden image data could not be reconstructed. The data may be corrupted."
                    )
                }
                return
            }
        }

        revealedImageBitmap = bitmap
        revealedImageBytes = imageBytes
        policyEnforcer.onRevealCommitted(meta, reveal.fingerprint)

        _uiState.update {
            it.copy(
                isDecoding = false,
                decodingResult = DecodingResult(
                    text = reveal.decoded.text,
                    hasImage = bitmap != null,
                    meta = meta
                ),
                countdownRemainingSeconds = meta.flags.selfDestructSeconds,
                errorMessage = if (meta.flags.deleteEncodedAfterReveal) {
                    "Encoded source image will be deleted when this reveal session ends."
                } else {
                    null
                },
                revealMasked = false
            )
        }
        pendingDeleteAfterSessionUri = if (meta.flags.deleteEncodedAfterReveal) reveal.encodedUri else null
        pendingReveal = null
        startCountdownIfNeeded(meta)
    }

    private fun startCountdownIfNeeded(meta: GhostMeta) {
        timerJob?.cancel()
        val duration = meta.flags.selfDestructSeconds ?: return
        timerJob = viewModelScope.launch {
            for (remaining in duration downTo 1) {
                _uiState.update { it.copy(countdownRemainingSeconds = remaining) }
                delay(1_000)
            }
            clearRevealSession("Secret cleared after the self-destruct timer ended.")
        }
    }

    fun saveRevealedImage() {
        val bytes = revealedImageBytes ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                FileSaveManager.saveImageBytesToGallery(
                    getApplication(),
                    bytes,
                    "ghostmask_revealed_${System.currentTimeMillis()}.jpg"
                )
                _uiState.update { it.copy(isSaving = false, savedRevealedImage = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to save image: ${e.message}") }
            }
        }
    }

    private suspend fun deleteEncodedFileIfNeeded(existingMessage: String?): String? {
        val uri = pendingDeleteAfterSessionUri ?: return existingMessage
        pendingDeleteAfterSessionUri = null
        return try {
            val resolver = getApplication<Application>().contentResolver
            val deleted = try {
                resolver.delete(uri, null, null) > 0
            } catch (_: SecurityException) {
                try {
                    DocumentsContract.deleteDocument(resolver, uri)
                } catch (_: Exception) {
                    false
                }
            }
            if (deleted) {
                "Encoded source image deleted after reveal."
            } else {
                existingMessage ?: "Secret opened, but GhostMask does not have permission to delete that selected file."
            }
        } catch (_: Exception) {
            existingMessage ?: "Secret opened, but GhostMask does not have permission to delete that selected file."
        }
    }

    fun onAppBackgrounded() {
        val meta = _uiState.value.decodingResult?.meta ?: return
        if (meta.flags.clearOnBackground) {
            viewModelScope.launch {
                clearRevealSession("Secret cleared when the app moved to the background.")
            }
        } else if (meta.flags.hideFromRecents) {
            _uiState.update { it.copy(revealMasked = true) }
        }
    }

    fun onAppForegrounded() {
        if (_uiState.value.decodingResult != null) {
            _uiState.update { it.copy(revealMasked = false) }
        }
    }

    fun panic() {
        viewModelScope.launch {
            clearRevealSession("Secret hidden.")
        }
    }

    fun closeRevealIfNeeded() {
        val shouldClear = _uiState.value.decodingResult?.meta?.flags?.clearOnClose == true ||
            _uiState.value.decodingResult != null
        if (!shouldClear) return

        viewModelScope.launch {
            clearRevealSession()
        }
    }

    fun reset() {
        viewModelScope.launch {
            clearRevealSession()
            encodedBitmap = null
            _uiState.value = RevealSecretsUiState()
        }
    }

    private suspend fun clearRevealSession(message: String? = null) {
        timerJob?.cancel()
        pendingReveal = null
        revealedImageBitmap = null
        revealedImageBytes = null
        policyEnforcer.clearSensitiveArtifacts()
        val finalMessage = deleteEncodedFileIfNeeded(message)
        _uiState.update {
            it.copy(
                isDecoding = false,
                isSaving = false,
                decodingResult = null,
                savedRevealedImage = false,
                countdownRemainingSeconds = null,
                pendingBiometricPrompt = false,
                pendingDeleteConfirmation = false,
                revealMasked = false,
                errorMessage = finalMessage ?: it.errorMessage
            )
        }
    }

    private fun fingerprint(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}

data class RevealSecretsUiState(
    val encodedImageUri: Uri? = null,
    val password: String = "",
    val isLoading: Boolean = false,
    val isDecoding: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val decodingResult: DecodingResult? = null,
    val savedRevealedImage: Boolean = false,
    val countdownRemainingSeconds: Int? = null,
    val pendingBiometricPrompt: Boolean = false,
    val pendingDeleteConfirmation: Boolean = false,
    val revealMasked: Boolean = false
)

data class DecodingResult(
    val text: String?,
    val hasImage: Boolean,
    val meta: GhostMeta
)

private data class PendingReveal(
    val decoded: com.ghostgramlabs.ghostmask.stego.DecodedPayload,
    val fingerprint: String,
    val encodedUri: Uri?
)
