package com.ghostgramlabs.ghostmask.stego

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builds a binary payload for GhostMask steganography.
 *
 * Binary format:
 *   [3 bytes] Magic header "GM1"
 *   [1 byte]  Version (currently 0x01)
 *   [1 byte]  Flags (bit 0 = hasText, bit 1 = hasImage)
 *   [4 bytes] Text length (big-endian int, 0 if no text)
 *   [4 bytes] Image length (big-endian int, 0 if no image)
 *   [N bytes] Encrypted data blob (salt + iv + ciphertext)
 *
 * The encrypted data blob contains [textBytes][imageBytes] concatenated
 * before encryption. Lengths in the header allow splitting after decryption.
 */
object PayloadBuilder {

    private val MAGIC = byteArrayOf('G'.code.toByte(), 'M'.code.toByte(), '1'.code.toByte())
    private const val VERSION: Byte = 0x01
    const val HEADER_SIZE = 3 + 1 + 1 + 4 + 4 // 13 bytes

    private const val FLAG_HAS_TEXT: Int = 0x01
    private const val FLAG_HAS_IMAGE: Int = 0x02

    /**
     * Builds a complete payload ready for steganographic embedding.
     *
     * @param text Optional secret text (UTF-8 encoded)
     * @param imageBytes Optional secret image as compressed byte array
     * @param password Required encryption password
     * @return Complete binary payload including header and encrypted data
     * @throws IllegalArgumentException if neither text nor image is provided
     */
    fun build(text: String?, imageBytes: ByteArray?, password: String): ByteArray {
        require(!text.isNullOrEmpty() || (imageBytes != null && imageBytes.isNotEmpty())) {
            "At least one secret (text or image) must be provided"
        }

        val textData = text?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        val imgData = imageBytes ?: ByteArray(0)

        // Combine raw data: [textBytes][imageBytes]
        val combined = ByteArray(textData.size + imgData.size)
        System.arraycopy(textData, 0, combined, 0, textData.size)
        System.arraycopy(imgData, 0, combined, textData.size, imgData.size)

        // Encrypt the combined payload
        val encrypted = CryptoEngine.encrypt(combined, password)
        val encryptedBytes = encrypted.toByteArray()

        // Build flags
        var flags = 0
        if (textData.isNotEmpty()) flags = flags or FLAG_HAS_TEXT
        if (imgData.isNotEmpty()) flags = flags or FLAG_HAS_IMAGE

        // Assemble final payload: header + encrypted blob
        val buffer = ByteBuffer.allocate(HEADER_SIZE + encryptedBytes.size)
            .order(ByteOrder.BIG_ENDIAN)
            .put(MAGIC)
            .put(VERSION)
            .put(flags.toByte())
            .putInt(textData.size)
            .putInt(imgData.size)
            .put(encryptedBytes)

        return buffer.array()
    }
}

/**
 * Parses a GhostMask binary payload extracted from a steganographic image.
 */
object PayloadParser {

    private val MAGIC = byteArrayOf('G'.code.toByte(), 'M'.code.toByte(), '1'.code.toByte())

    /**
     * Parses the payload and decrypts the secrets.
     *
     * @param payload Raw bytes extracted from the stego image
     * @param password Decryption password
     * @return [DecodedPayload] containing any recovered text and/or image
     * @throws InvalidPayloadException if the payload format is invalid
     * @throws WrongPasswordException if the password is incorrect
     */
    fun parse(payload: ByteArray, password: String): DecodedPayload {
        if (payload.size < PayloadBuilder.HEADER_SIZE) {
            throw InvalidPayloadException("Payload too short to contain a valid header")
        }

        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)

        // Validate magic header
        val magic = ByteArray(3)
        buffer.get(magic)
        if (!magic.contentEquals(MAGIC)) {
            throw InvalidPayloadException("No GhostMask data found in this image")
        }

        // Read version
        val version = buffer.get()
        if (version != 0x01.toByte()) {
            throw InvalidPayloadException("Unsupported GhostMask version: $version")
        }

        // Read flags and lengths
        val flags = buffer.get().toInt() and 0xFF
        val textLength = buffer.getInt()
        val imageLength = buffer.getInt()

        val hasText = (flags and 0x01) != 0
        val hasImage = (flags and 0x02) != 0

        // Validate lengths
        if (textLength < 0 || imageLength < 0) {
            throw InvalidPayloadException("Invalid payload lengths")
        }

        // Extract encrypted blob
        val encryptedBytes = ByteArray(buffer.remaining())
        buffer.get(encryptedBytes)

        // Decrypt
        val encryptedPackage = EncryptedPackage.fromByteArray(encryptedBytes)
        val decrypted = CryptoEngine.decrypt(encryptedPackage, password)

        // Validate decrypted size matches expected lengths
        if (decrypted.size != textLength + imageLength) {
            throw InvalidPayloadException(
                "Decrypted data size mismatch: expected ${textLength + imageLength}, got ${decrypted.size}"
            )
        }

        // Split into text and image portions
        val text = if (hasText && textLength > 0) {
            String(decrypted, 0, textLength, Charsets.UTF_8)
        } else null

        val imageBytes = if (hasImage && imageLength > 0) {
            decrypted.copyOfRange(textLength, textLength + imageLength)
        } else null

        return DecodedPayload(text = text, imageBytes = imageBytes)
    }
}

/**
 * Result of decoding and decrypting a GhostMask payload.
 */
data class DecodedPayload(
    val text: String?,
    val imageBytes: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecodedPayload) return false
        return text == other.text && imageBytes.contentEquals(other.imageBytes)
    }

    override fun hashCode(): Int {
        var result = text?.hashCode() ?: 0
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Thrown when the payload format is invalid or unrecognizable.
 */
class InvalidPayloadException(message: String) : Exception(message)
