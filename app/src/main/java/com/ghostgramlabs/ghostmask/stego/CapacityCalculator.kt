package com.ghostgramlabs.ghostmask.stego

/**
 * Calculates the steganographic capacity of a cover image.
 *
 * Each pixel contributes 3 usable bits (one per R, G, B channel LSB).
 * Alpha channel is preserved and not used for encoding.
 * The first 32 bits (4 bytes) are reserved for the payload size header.
 */
object CapacityCalculator {

    /** Bytes reserved at the start for encoding the payload length */
    private const val SIZE_HEADER_BYTES = 4

    /**
     * Returns the maximum payload size in bytes that can be hidden
     * in an image of the given dimensions.
     */
    fun calculateCapacity(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 0
        val totalBits = width.toLong() * height.toLong() * 3L
        val totalBytes = (totalBits / 8L).toInt()
        return maxOf(0, totalBytes - SIZE_HEADER_BYTES)
    }

    /**
     * Checks whether a payload of [payloadSize] bytes can fit
     * inside an image of the given dimensions.
     */
    fun canFit(width: Int, height: Int, payloadSize: Int): Boolean {
        return payloadSize <= calculateCapacity(width, height)
    }

    /**
     * Returns a human-readable capacity summary.
     */
    fun capacitySummary(width: Int, height: Int, payloadSize: Int): CapacityInfo {
        val capacity = calculateCapacity(width, height)
        val usageRatio = if (capacity > 0) payloadSize.toFloat() / capacity else 1f
        return CapacityInfo(
            totalBytes = capacity,
            usedBytes = payloadSize,
            remainingBytes = maxOf(0, capacity - payloadSize),
            usageRatio = usageRatio.coerceIn(0f, 1f),
            fits = payloadSize <= capacity
        )
    }
}

/**
 * Describes payload capacity usage for a cover image.
 */
data class CapacityInfo(
    val totalBytes: Int,
    val usedBytes: Int,
    val remainingBytes: Int,
    val usageRatio: Float,
    val fits: Boolean
)
