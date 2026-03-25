package com.ghostgramlabs.ghostmask.stego

import android.graphics.Bitmap

/**
 * Encodes a byte array payload into the least significant bits (LSB)
 * of a cover image's RGB channels. Alpha channel is preserved.
 *
 * Encoding order:
 *   1. First 32 bits encode the payload length as a big-endian int
 *   2. Remaining bits encode the payload data
 *
 * Bit layout per pixel: R-LSB, G-LSB, B-LSB (3 bits per pixel)
 */
object StegoEncoder {

    /**
     * Encodes [payload] into a mutable copy of [coverBitmap].
     *
     * @param coverBitmap The original cover image (not modified)
     * @param payload The bytes to hide
     * @return A new Bitmap with the payload embedded
     * @throws PayloadTooLargeException if the payload exceeds capacity
     */
    fun encode(coverBitmap: Bitmap, payload: ByteArray): Bitmap {
        val width = coverBitmap.width
        val height = coverBitmap.height

        if (!CapacityCalculator.canFit(width, height, payload.size)) {
            val capacity = CapacityCalculator.calculateCapacity(width, height)
            throw PayloadTooLargeException(
                "Payload (${payload.size} bytes) exceeds cover image capacity ($capacity bytes). " +
                        "Need ${payload.size - capacity} more bytes of capacity."
            )
        }

        // Create mutable copy
        val result = coverBitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Prepare bit stream: [4 bytes length][payload bytes]
        val lengthBytes = byteArrayOf(
            (payload.size shr 24 and 0xFF).toByte(),
            (payload.size shr 16 and 0xFF).toByte(),
            (payload.size shr 8 and 0xFF).toByte(),
            (payload.size and 0xFF).toByte()
        )
        val fullData = lengthBytes + payload

        val writer = BitmapBitWriter(result)
        for (byte in fullData) {
            for (bit in 7 downTo 0) {
                val bitValue = (byte.toInt() shr bit) and 1
                writer.writeBit(bitValue)
            }
        }

        return result
    }
}

/**
 * Decodes a byte array payload from the LSBs of a stego image's RGB channels.
 */
object StegoDecoder {

    /**
     * Extracts hidden payload from [stegoBitmap].
     *
     * @param stegoBitmap The image containing hidden data
     * @return The extracted payload bytes
     * @throws InvalidPayloadException if the image doesn't contain valid data
     */
    fun decode(stegoBitmap: Bitmap): ByteArray {
        val reader = BitmapBitReader(stegoBitmap)

        // Read 4-byte length header (32 bits)
        var length = 0
        for (i in 0 until 32) {
            length = (length shl 1) or reader.readBit()
        }

        // Sanity check the length
        val maxCapacity = CapacityCalculator.calculateCapacity(stegoBitmap.width, stegoBitmap.height)
        if (length < 0 || length > maxCapacity) {
            throw InvalidPayloadException(
                "Invalid payload length ($length). This image may not contain GhostMask data."
            )
        }

        // Read payload bytes
        val payload = ByteArray(length)
        for (i in 0 until length) {
            var byte = 0
            for (bit in 7 downTo 0) {
                byte = (byte shl 1) or reader.readBit()
            }
            payload[i] = byte.toByte()
        }

        return payload
    }
}

/**
 * Writes individual bits into the LSBs of a Bitmap's RGB channels.
 * Iterates pixels left-to-right, top-to-bottom: R, G, B per pixel.
 */
class BitmapBitWriter(private val bitmap: Bitmap) {
    private var pixelIndex = 0
    private var channelIndex = 0 // 0=R, 1=G, 2=B
    private val totalPixels = bitmap.width * bitmap.height

    fun writeBit(bit: Int) {
        if (pixelIndex >= totalPixels) {
            throw PayloadTooLargeException("Ran out of pixels while encoding")
        }

        val x = pixelIndex % bitmap.width
        val y = pixelIndex / bitmap.width
        val pixel = bitmap.getPixel(x, y)

        val a = (pixel shr 24) and 0xFF
        var r = (pixel shr 16) and 0xFF
        var g = (pixel shr 8) and 0xFF
        var b = pixel and 0xFF

        when (channelIndex) {
            0 -> r = (r and 0xFE) or bit
            1 -> g = (g and 0xFE) or bit
            2 -> b = (b and 0xFE) or bit
        }

        bitmap.setPixel(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)

        channelIndex++
        if (channelIndex > 2) {
            channelIndex = 0
            pixelIndex++
        }
    }
}

/**
 * Reads individual bits from the LSBs of a Bitmap's RGB channels.
 * Same iteration order as [BitmapBitWriter]: left-to-right, top-to-bottom, R→G→B.
 */
class BitmapBitReader(private val bitmap: Bitmap) {
    private var pixelIndex = 0
    private var channelIndex = 0
    private val totalPixels = bitmap.width * bitmap.height

    fun readBit(): Int {
        if (pixelIndex >= totalPixels) {
            throw InvalidPayloadException("Ran out of pixels while decoding")
        }

        val x = pixelIndex % bitmap.width
        val y = pixelIndex / bitmap.width
        val pixel = bitmap.getPixel(x, y)

        val value = when (channelIndex) {
            0 -> (pixel shr 16) and 1 // R LSB
            1 -> (pixel shr 8) and 1  // G LSB
            2 -> pixel and 1          // B LSB
            else -> throw IllegalStateException()
        }

        channelIndex++
        if (channelIndex > 2) {
            channelIndex = 0
            pixelIndex++
        }

        return value
    }
}

/**
 * Thrown when the payload is too large for the cover image.
 */
class PayloadTooLargeException(message: String) : Exception(message)
