package com.ghostgramlabs.ghostmask.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgramlabs.ghostmask.stego.*
import com.ghostgramlabs.ghostmask.util.BitmapUtils
import com.ghostgramlabs.ghostmask.util.FileSaveManager
import com.ghostgramlabs.ghostmask.util.ShareManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Hide Secrets screen.
 * Manages cover image selection, secret inputs, capacity calculation,
 * encoding, and result saving/sharing.
 */
class HideSecretsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HideSecretsUiState())
    val uiState: StateFlow<HideSecretsUiState> = _uiState.asStateFlow()

    // Keep bitmaps separate from UI state (not serializable)
    private var coverBitmap: Bitmap? = null
    private var secretImageBitmap: Bitmap? = null
    private var encodedBitmap: Bitmap? = null

    fun getCoverBitmap(): Bitmap? = coverBitmap
    fun getSecretImageBitmap(): Bitmap? = secretImageBitmap
    fun getEncodedBitmap(): Bitmap? = encodedBitmap

    /**
     * Sets the cover image from a content URI.
     * Loads the bitmap and recalculates capacity.
     */
    fun setCoverImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val bitmap = BitmapUtils.loadBitmap(getApplication(), uri)
                if (bitmap == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load cover image") }
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

    /**
     * Sets the secret text input.
     */
    fun setSecretText(text: String) {
        _uiState.update { it.copy(secretText = text, encodingResult = null) }
        recalculateCapacity()
    }

    /**
     * Sets the secret image from a content URI.
     * Loads and compresses the image, then recalculates capacity.
     */
    fun setSecretImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Load with max dimension 1024 to keep payload reasonable
                val bitmap = BitmapUtils.loadBitmap(getApplication(), uri, maxDimension = 1024)
                if (bitmap == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load secret image") }
                    return@launch
                }
                secretImageBitmap = bitmap
                _uiState.update {
                    it.copy(
                        secretImageUri = uri,
                        isLoading = false,
                        encodingResult = null
                    )
                }
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

    fun clearSecretText() {
        _uiState.update { it.copy(secretText = "", encodingResult = null) }
        recalculateCapacity()
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password, encodingResult = null) }
    }

    /**
     * Recalculates capacity based on current cover image and payload.
     */
    private fun recalculateCapacity() {
        val state = _uiState.value
        val cover = coverBitmap ?: return

        val textBytes = if (state.secretText.isNotEmpty()) {
            state.secretText.toByteArray(Charsets.UTF_8).size
        } else 0

        val imageBytes = secretImageBitmap?.let {
            ImageCompressionHelper.compressImage(it).size
        } ?: 0

        // Estimate payload size: header + salt(16) + iv(12) + textBytes + imageBytes + authTag(16)
        val estimatedPayload = PayloadBuilder.HEADER_SIZE + 16 + 12 + textBytes + imageBytes + 16

        val capacityInfo = CapacityCalculator.capacitySummary(
            cover.width, cover.height, estimatedPayload
        )

        _uiState.update { it.copy(capacityInfo = capacityInfo) }
    }

    /**
     * Encodes the secrets into the cover image.
     */
    fun encode() {
        val state = _uiState.value
        val cover = coverBitmap

        // Validate inputs
        if (cover == null) {
            _uiState.update { it.copy(errorMessage = "Please select a cover image") }
            return
        }
        if (state.secretText.isEmpty() && secretImageBitmap == null) {
            _uiState.update { it.copy(errorMessage = "Please provide at least one secret (text or image)") }
            return
        }
        if (state.password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Password is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isEncoding = true, errorMessage = null, encodingResult = null) }
            try {
                val text = state.secretText.ifEmpty { null }
                val imageBytes = secretImageBitmap?.let {
                    ImageCompressionHelper.compressImage(it)
                }

                // Build the encrypted payload
                val payload = PayloadBuilder.build(text, imageBytes, state.password)

                // Embed into cover image
                val result = StegoEncoder.encode(cover, payload)
                encodedBitmap = result

                _uiState.update {
                    it.copy(
                        isEncoding = false,
                        encodingResult = EncodingResult.Success
                    )
                }
            } catch (e: PayloadTooLargeException) {
                _uiState.update {
                    it.copy(isEncoding = false, errorMessage = e.message)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isEncoding = false, errorMessage = "Encoding failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Saves the encoded image to gallery.
     */
    fun saveToGallery() {
        val bitmap = encodedBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val fileName = "ghostmask_${System.currentTimeMillis()}.png"
                val uri = FileSaveManager.savePngToGallery(getApplication(), bitmap, fileName)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        encodingResult = EncodingResult.Saved(uri)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Save failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Creates a share intent for the encoded image.
     */
    fun share(onIntentReady: (android.content.Intent) -> Unit) {
        val bitmap = encodedBitmap ?: return
        viewModelScope.launch {
            try {
                val fileName = "ghostmask_${System.currentTimeMillis()}.png"
                val intent = ShareManager.createShareIntent(getApplication(), bitmap, fileName)
                onIntentReady(intent)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Share failed: ${e.message}") }
            }
        }
    }

    /**
     * Resets the screen to initial state.
     */
    fun reset() {
        coverBitmap = null
        secretImageBitmap = null
        encodedBitmap = null
        _uiState.value = HideSecretsUiState()
    }
}

/**
 * UI state for the Hide Secrets screen.
 */
data class HideSecretsUiState(
    val coverImageUri: Uri? = null,
    val coverWidth: Int = 0,
    val coverHeight: Int = 0,
    val secretText: String = "",
    val secretImageUri: Uri? = null,
    val password: String = "",
    val capacityInfo: CapacityInfo? = null,
    val isLoading: Boolean = false,
    val isEncoding: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val encodingResult: EncodingResult? = null
)

sealed class EncodingResult {
    data object Success : EncodingResult()
    data class Saved(val uri: Uri) : EncodingResult()
}
