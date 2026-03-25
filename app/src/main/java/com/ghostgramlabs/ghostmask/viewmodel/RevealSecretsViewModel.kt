package com.ghostgramlabs.ghostmask.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgramlabs.ghostmask.stego.*
import com.ghostgramlabs.ghostmask.util.BitmapUtils
import com.ghostgramlabs.ghostmask.util.FileSaveManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Reveal Secrets screen.
 * Manages encoded image loading, decoding, and secret image saving.
 */
class RevealSecretsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RevealSecretsUiState())
    val uiState: StateFlow<RevealSecretsUiState> = _uiState.asStateFlow()

    private var encodedBitmap: Bitmap? = null
    private var revealedImageBitmap: Bitmap? = null
    private var revealedImageBytes: ByteArray? = null

    fun getEncodedBitmap(): Bitmap? = encodedBitmap
    fun getRevealedImageBitmap(): Bitmap? = revealedImageBitmap

    /**
     * Sets the encoded image from a content URI.
     * Must be loaded at full resolution to preserve LSB data.
     */
    fun setEncodedImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, decodingResult = null) }
            try {
                // Load at full resolution — LSB data requires exact pixels
                val bitmap = BitmapUtils.loadBitmap(getApplication(), uri, maxDimension = 0)
                if (bitmap == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load image") }
                    return@launch
                }
                encodedBitmap = bitmap
                _uiState.update {
                    it.copy(encodedImageUri = uri, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Error loading image: ${e.message}")
                }
            }
        }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password, decodingResult = null) }
    }

    /**
     * Decodes and decrypts secrets from the encoded image.
     */
    fun decode() {
        val bitmap = encodedBitmap
        val password = _uiState.value.password

        if (bitmap == null) {
            _uiState.update { it.copy(errorMessage = "Please select an encoded image") }
            return
        }
        if (password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Password is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDecoding = true, errorMessage = null, decodingResult = null) }
            try {
                // Extract embedded payload
                val rawPayload = StegoDecoder.decode(bitmap)

                // Parse and decrypt
                val decoded = PayloadParser.parse(rawPayload, password)

                // Reconstruct secret image if present
                var imgBitmap: Bitmap? = null
                if (decoded.imageBytes != null) {
                    imgBitmap = ImageCompressionHelper.decompressImage(decoded.imageBytes)
                    if (imgBitmap == null) {
                        _uiState.update {
                            it.copy(
                                isDecoding = false,
                                errorMessage = "Hidden image data could not be reconstructed. The data may be corrupted."
                            )
                        }
                        return@launch
                    }
                    revealedImageBitmap = imgBitmap
                    revealedImageBytes = decoded.imageBytes
                }

                _uiState.update {
                    it.copy(
                        isDecoding = false,
                        decodingResult = DecodingResult(
                            text = decoded.text,
                            hasImage = imgBitmap != null
                        )
                    )
                }
            } catch (e: WrongPasswordException) {
                _uiState.update {
                    it.copy(isDecoding = false, errorMessage = "Wrong password. Please try again.")
                }
            } catch (e: InvalidPayloadException) {
                _uiState.update {
                    it.copy(isDecoding = false, errorMessage = e.message ?: "Invalid data in image")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isDecoding = false, errorMessage = "Decoding failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Saves the revealed secret image to gallery.
     */
    fun saveRevealedImage() {
        val bytes = revealedImageBytes ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val fileName = "ghostmask_revealed_${System.currentTimeMillis()}.jpg"
                FileSaveManager.saveImageBytesToGallery(getApplication(), bytes, fileName)
                _uiState.update {
                    it.copy(isSaving = false, savedRevealedImage = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Failed to save image: ${e.message}")
                }
            }
        }
    }

    /**
     * Resets the screen to initial state.
     */
    fun reset() {
        encodedBitmap = null
        revealedImageBitmap = null
        revealedImageBytes = null
        _uiState.value = RevealSecretsUiState()
    }
}

/**
 * UI state for the Reveal Secrets screen.
 */
data class RevealSecretsUiState(
    val encodedImageUri: Uri? = null,
    val password: String = "",
    val isLoading: Boolean = false,
    val isDecoding: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val decodingResult: DecodingResult? = null,
    val savedRevealedImage: Boolean = false
)

data class DecodingResult(
    val text: String?,
    val hasImage: Boolean
)
