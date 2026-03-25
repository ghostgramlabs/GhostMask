package com.ghostgramlabs.ghostmask.stego

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Compresses and decompresses secret images for payload embedding.
 * Uses JPEG compression to minimize payload size while maintaining
 * acceptable image quality.
 */
object ImageCompressionHelper {

    /** Default JPEG quality (0-100). Balanced between size and quality. */
    const val DEFAULT_QUALITY = 60

    /**
     * Compresses a [Bitmap] to a JPEG byte array.
     *
     * @param bitmap The image to compress
     * @param quality JPEG quality (0-100), default [DEFAULT_QUALITY]
     * @return Compressed image bytes
     */
    fun compressImage(bitmap: Bitmap, quality: Int = DEFAULT_QUALITY): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), stream)
        return stream.toByteArray()
    }

    /**
     * Decompresses JPEG bytes back into a [Bitmap].
     *
     * @param bytes Compressed image data
     * @return Decoded Bitmap, or null if bytes are invalid
     */
    fun decompressImage(bytes: ByteArray): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
