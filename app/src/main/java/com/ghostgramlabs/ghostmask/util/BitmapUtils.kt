package com.ghostgramlabs.ghostmask.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility functions for loading bitmaps from URIs with memory safety.
 */
object BitmapUtils {

    /**
     * Loads a bitmap from a content URI, optionally downsampling if it exceeds
     * [maxDimension] on either axis. This prevents OOM on very large images.
     *
     * @param context Application context
     * @param uri Content URI of the image
     * @param maxDimension Maximum width or height (0 = no limit)
     * @return Decoded bitmap in ARGB_8888 config, or null on failure
     */
    suspend fun loadBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 0
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver

            if (maxDimension > 0) {
                // First pass: decode bounds only
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(
                    options.outWidth, options.outHeight, maxDimension
                )
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.ARGB_8888

                resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            } else {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates the optimal sample size for downsampling.
     */
    private fun calculateInSampleSize(
        rawWidth: Int, rawHeight: Int, maxDimension: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > maxDimension || rawWidth > maxDimension) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
