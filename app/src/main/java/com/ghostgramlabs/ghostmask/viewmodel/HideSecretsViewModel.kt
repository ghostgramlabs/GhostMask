package com.ghostgramlabs.ghostmask.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgramlabs.ghostmask.data.settings.AppSettingsRepository
import com.ghostgramlabs.ghostmask.domain.model.EmbeddingMetadata
import com.ghostgramlabs.ghostmask.domain.model.EmbeddingMode
import com.ghostgramlabs.ghostmask.domain.model.GhostMeta
import com.ghostgramlabs.ghostmask.domain.model.GhostPayload
import com.ghostgramlabs.ghostmask.domain.model.PayloadType
import com.ghostgramlabs.ghostmask.domain.model.RevealFlags
import com.ghostgramlabs.ghostmask.stego.CapacityCalculator
import com.ghostgramlabs.ghostmask.stego.CapacityInfo
import com.ghostgramlabs.ghostmask.stego.ImageCompressionHelper
import com.ghostgramlabs.ghostmask.stego.PayloadBuilder
import com.ghostgramlabs.ghostmask.stego.PayloadTooLargeException
import com.ghostgramlabs.ghostmask.stego.StegoEncoder
import com.ghostgramlabs.ghostmask.util.BitmapUtils
import com.ghostgramlabs.ghostmask.util.FileSaveManager
import com.ghostgramlabs.ghostmask.util.ShareManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HideSecretsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = AppSettingsRepository(application)
    private val _uiState = MutableStateFlow(HideSecretsUiState())
    val uiState: StateFlow<HideSecretsUiState> = _uiState.asStateFlow()

    private var coverBitmap: Bitmap? = null
    private var secretImageBitmap: Bitmap? = null
    private var encodedBitmap: Bitmap? = null
    private var defaultsApplied = false
    private var currentDefaultSecureView = true
    private var currentDefaultScreenshotBlocking = true

    fun getEncodedBitmap(): Bitmap? = encodedBitmap

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                currentDefaultSecureView = settings.defaultSecureView
                currentDefaultScreenshotBlocking = settings.defaultScreenshotBlocking
                if (!defaultsApplied) {
                    _uiState.update {
                        it.copy(
                            revealFlags = it.revealFlags.copy(
                                secureView = settings.defaultSecureView,
                                blockScreenshots = settings.defaultScreenshotBlocking
                            )
                        )
                    }
                    defaultsApplied = true
                }
            }
        }
    }

    fun setCoverImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val bitmap = BitmapUtils.loadBitmap(getApplication(), uri)
                if (bitmap == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load cover image.") }
                    return@launch
                }
                coverBitmap = bitmap
                _uiState.update {
                    it.copy(
                        coverImageUri = uri,
                        coverWidth = bitmap.width,
                        coverHeight = bitmap.height,
                        isLoading = false,
                        encodingResult = null
                    )
                }
                recalculateCapacity()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error loading image: ${e.message}") }
            }
        }
    }

    fun setPayloadType(payloadType: PayloadType) {
        _uiState.update { it.copy(payloadType = payloadType, encodingResult = null, errorMessage = null) }
        recalculateCapacity()
    }

    fun setSecretText(text: String) {
        _uiState.update { it.copy(secretText = text, encodingResult = null) }
        recalculateCapacity()
    }

    fun setSecretImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val bitmap = BitmapUtils.loadBitmap(getApplication(), uri, maxDimension = 1024)
                if (bitmap == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load secret image.") }
                    return@launch
                }
                secretImageBitmap = bitmap
                _uiState.update { it.copy(secretImageUri = uri, isLoading = false, encodingResult = null) }
                recalculateCapacity()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error loading secret image: ${e.message}") }
            }
        }
    }

    fun clearSecretImage() {
        secretImageBitmap = null
        _uiState.update { it.copy(secretImageUri = null, encodingResult = null) }
        recalculateCapacity()
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password, encodingResult = null) }
        recalculateCapacity()
    }

    fun setConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, encodingResult = null) }
    }

    fun setCompressionEnabled(enabled: Boolean) {
        _uiState.update { it.copy(compressionEnabled = enabled, encodingResult = null) }
        recalculateCapacity()
    }

    fun setExpiryEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                expiryEnabled = enabled,
                expiryEpochMs = if (enabled) it.expiryEpochMs ?: (System.currentTimeMillis() + DEFAULT_EXPIRY_MS) else null,
                encodingResult = null
            )
        }
    }

    fun setExpiryPreset(label: String, durationMs: Long?) {
        _uiState.update {
            it.copy(
                expiryPresetLabel = label,
                expiryEnabled = durationMs != null,
                expiryEpochMs = durationMs?.let { duration -> System.currentTimeMillis() + duration },
                encodingResult = null
            )
        }
    }

    fun setSenderLabel(label: String) {
        _uiState.update { it.copy(senderLabel = label, encodingResult = null) }
        recalculateCapacity()
    }

    fun setOutputFileName(name: String) {
        _uiState.update { it.copy(outputFileName = name, encodingResult = null) }
    }

    fun setEmbeddingMode(mode: EmbeddingMode) {
        _uiState.update { it.copy(embeddingMode = mode, encodingResult = null) }
        recalculateCapacity()
    }

    fun setSecureView(enabled: Boolean) = updateFlags { copy(secureView = enabled) }
    fun setBlockScreenshots(enabled: Boolean) = updateFlags { copy(blockScreenshots = enabled) }
    fun setHideFromRecents(enabled: Boolean) = updateFlags { copy(hideFromRecents = enabled) }
    fun setClearOnClose(enabled: Boolean) = updateFlags { copy(clearOnClose = enabled) }
    fun setClearOnBackground(enabled: Boolean) = updateFlags { copy(clearOnBackground = enabled) }
    fun setOneTimeReveal(enabled: Boolean) = updateFlags { copy(oneTimeReveal = enabled) }
    fun setRequireBiometric(enabled: Boolean) = updateFlags { copy(requireBiometric = enabled) }
    fun setDeleteEncodedAfterReveal(enabled: Boolean) = updateFlags { copy(deleteEncodedAfterReveal = enabled) }

    fun setSelfDestructEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                selfDestructEnabled = enabled,
                selfDestructSeconds = if (enabled) it.selfDestructSeconds ?: 30 else null,
                encodingResult = null
            )
        }
    }

    fun setSelfDestructSeconds(seconds: Int?) {
        _uiState.update { it.copy(selfDestructSeconds = seconds, encodingResult = null) }
    }

    private fun updateFlags(transform: RevealFlagsUiState.() -> RevealFlagsUiState) {
        _uiState.update {
            it.copy(revealFlags = it.revealFlags.transform(), encodingResult = null)
        }
    }

    private fun recalculateCapacity() {
        val state = _uiState.value
        val cover = coverBitmap ?: return
        val payload = buildGhostPayloadOrNull(state) ?: run {
            _uiState.update { it.copy(capacityInfo = null) }
            return
        }

        val previewPassword = if (state.password.isNotEmpty()) state.password else "ghostmask-preview"
        val estimatedPayload = PayloadBuilder.build(payload, previewPassword).size
        val capacityInfo = CapacityCalculator.capacitySummary(
            width = cover.width,
            height = cover.height,
            payloadSize = estimatedPayload,
            lsbBits = state.embeddingMode.lsbBits
        )
        _uiState.update { it.copy(capacityInfo = capacityInfo) }
    }

    fun encode() {
        val state = _uiState.value
        val cover = coverBitmap

        if (cover == null) {
            _uiState.update { it.copy(errorMessage = "Please select a cover image.") }
            return
        }

        val validationError = validate(state)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        val payload = buildGhostPayloadOrNull(state)
        if (payload == null) {
            _uiState.update { it.copy(errorMessage = "Please provide the required secret content.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isEncoding = true, errorMessage = null, encodingResult = null) }
            try {
                val wrappedPayload = PayloadBuilder.build(payload, state.password)
                val result = StegoEncoder.encode(cover, wrappedPayload, state.embeddingMode.lsbBits)
                encodedBitmap = result
                _uiState.update { it.copy(isEncoding = false, encodingResult = EncodingResult.Success) }
            } catch (e: PayloadTooLargeException) {
                _uiState.update { it.copy(isEncoding = false, errorMessage = "This cover image is too small for the selected secret.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isEncoding = false, errorMessage = "Encoding failed: ${e.message}") }
            }
        }
    }

    fun saveOutput() {
        val bitmap = encodedBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val fileName = FileSaveManager.sanitizeOutputName(_uiState.value.outputFileName)
                val settings = settingsRepository.settings.first()
                val uri = if (settings.saveToPrivateStorage) {
                    FileSaveManager.savePngToPrivateStorage(getApplication(), bitmap, fileName).uri
                } else {
                    FileSaveManager.savePngToGallery(getApplication(), bitmap, fileName)
                }
                _uiState.update { it.copy(isSaving = false, encodingResult = EncodingResult.Saved(uri)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Save failed: ${e.message}") }
            }
        }
    }

    fun exportToGallery() {
        val bitmap = encodedBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val uri = FileSaveManager.savePngToGallery(
                    getApplication(),
                    bitmap,
                    FileSaveManager.sanitizeOutputName(_uiState.value.outputFileName)
                )
                _uiState.update { it.copy(isSaving = false, encodingResult = EncodingResult.Saved(uri)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun share(onIntentReady: (android.content.Intent) -> Unit) {
        val bitmap = encodedBitmap ?: return
        viewModelScope.launch {
            try {
                val fileName = FileSaveManager.sanitizeOutputName(_uiState.value.outputFileName)
                val intent = ShareManager.createShareIntent(getApplication(), bitmap, fileName)
                onIntentReady(intent)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Share failed: ${e.message}") }
            }
        }
    }

    fun sendEmail(onIntentReady: (Intent) -> Unit) {
        val bitmap = encodedBitmap ?: return
        viewModelScope.launch {
            try {
                val fileName = FileSaveManager.sanitizeOutputName(_uiState.value.outputFileName)
                onIntentReady(ShareManager.createEmailIntent(getApplication(), bitmap, fileName))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Email share failed: ${e.message}") }
            }
        }
    }

    fun openInFiles(onIntentReady: (Intent) -> Unit) {
        val bitmap = encodedBitmap ?: return
        viewModelScope.launch {
            try {
                val handle = FileSaveManager.savePngToPrivateStorage(
                    getApplication(),
                    bitmap,
                    FileSaveManager.sanitizeOutputName(_uiState.value.outputFileName)
                )
                onIntentReady(FileSaveManager.createOpenFileIntent(handle.uri))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Open in files failed: ${e.message}") }
            }
        }
    }

    fun reset() {
        coverBitmap = null
        secretImageBitmap = null
        encodedBitmap = null
        _uiState.value = HideSecretsUiState(
            revealFlags = RevealFlagsUiState(
                secureView = currentDefaultSecureView,
                blockScreenshots = currentDefaultScreenshotBlocking
            )
        )
    }

    private fun validate(state: HideSecretsUiState): String? {
        return when (state.payloadType) {
            PayloadType.TEXT -> if (state.secretText.isBlank()) "Secret text is required for text mode." else null
            PayloadType.IMAGE -> if (secretImageBitmap == null) "Secret image is required for image mode." else null
            PayloadType.BOTH -> when {
                state.secretText.isBlank() -> "Secret text is required for text + image mode."
                secretImageBitmap == null -> "Secret image is required for text + image mode."
                else -> null
            }
        } ?: when {
            state.password.length < MIN_PASSWORD_LENGTH -> "Use a password with at least $MIN_PASSWORD_LENGTH characters."
            state.password != state.confirmPassword -> "Passwords do not match."
            state.selfDestructEnabled && (state.selfDestructSeconds == null || state.selfDestructSeconds <= 0) ->
                "Choose a valid self-destruct timer."
            state.capacityInfo?.fits == false -> "This cover image is too small for the selected secret."
            else -> null
        }
    }

    private fun buildGhostPayloadOrNull(state: HideSecretsUiState): GhostPayload? {
        val textBytes = state.secretText.takeIf { it.isNotBlank() }?.toByteArray(Charsets.UTF_8)
        val imageBytes = secretImageBitmap?.let { ImageCompressionHelper.compressImage(it) }

        val payloadType = when (state.payloadType) {
            PayloadType.TEXT -> if (textBytes != null) PayloadType.TEXT else return null
            PayloadType.IMAGE -> if (imageBytes != null) PayloadType.IMAGE else return null
            PayloadType.BOTH -> if (textBytes != null && imageBytes != null) PayloadType.BOTH else return null
        }

        val flags = RevealFlags(
            secureView = state.revealFlags.secureView,
            blockScreenshots = state.revealFlags.blockScreenshots,
            hideFromRecents = state.revealFlags.hideFromRecents,
            clearOnClose = state.revealFlags.clearOnClose,
            clearOnBackground = state.revealFlags.clearOnBackground,
            selfDestructSeconds = if (state.selfDestructEnabled) state.selfDestructSeconds else null,
            oneTimeReveal = state.revealFlags.oneTimeReveal,
            requireBiometric = state.revealFlags.requireBiometric,
            deleteEncodedAfterReveal = state.revealFlags.deleteEncodedAfterReveal
        )

        return GhostPayload(
            meta = GhostMeta(
                payloadType = payloadType,
                createdAtEpochMs = System.currentTimeMillis(),
                textLength = textBytes?.size ?: 0,
                imageLength = imageBytes?.size ?: 0,
                compressionEnabled = state.compressionEnabled,
                expiryEpochMs = if (state.expiryEnabled) state.expiryEpochMs else null,
                flags = flags,
                senderLabel = state.senderLabel.takeIf { it.isNotBlank() },
                embedding = EmbeddingMetadata(lsbBitsUsed = state.embeddingMode.lsbBits)
            ),
            textBytes = textBytes,
            imageBytes = imageBytes
        )
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val DEFAULT_EXPIRY_MS = 24 * 60 * 60 * 1000L
    }
}

data class HideSecretsUiState(
    val coverImageUri: Uri? = null,
    val coverWidth: Int = 0,
    val coverHeight: Int = 0,
    val payloadType: PayloadType = PayloadType.TEXT,
    val secretText: String = "",
    val secretImageUri: Uri? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val senderLabel: String = "",
    val outputFileName: String = "ghostmask_secret",
    val compressionEnabled: Boolean = true,
    val expiryEnabled: Boolean = false,
    val expiryEpochMs: Long? = null,
    val expiryPresetLabel: String = "No expiry",
    val embeddingMode: EmbeddingMode = EmbeddingMode.STEALTH,
    val revealFlags: RevealFlagsUiState = RevealFlagsUiState(),
    val selfDestructEnabled: Boolean = false,
    val selfDestructSeconds: Int? = null,
    val capacityInfo: CapacityInfo? = null,
    val isLoading: Boolean = false,
    val isEncoding: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val encodingResult: EncodingResult? = null
)

data class RevealFlagsUiState(
    val secureView: Boolean = false,
    val blockScreenshots: Boolean = false,
    val hideFromRecents: Boolean = false,
    val clearOnClose: Boolean = false,
    val clearOnBackground: Boolean = false,
    val oneTimeReveal: Boolean = false,
    val requireBiometric: Boolean = false,
    val deleteEncodedAfterReveal: Boolean = false
)

sealed class EncodingResult {
    data object Success : EncodingResult()
    data class Saved(val uri: Uri) : EncodingResult()
}
