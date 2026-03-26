package com.ghostgramlabs.ghostmask.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Creates share intents for encoded PNG images using FileProvider.
 */
object ShareManager {

    /**
     * Creates a share [Intent] for the given encoded bitmap.
     * Saves the bitmap to cache first, then generates a secure content URI.
     *
     * @param context Application context
     * @param bitmap The encoded image to share
     * @param fileName Name for the shared file
     * @return A chooser intent ready to launch
     */
    suspend fun createShareIntent(context: Context, bitmap: Bitmap, fileName: String): Intent {
        return withContext(Dispatchers.IO) {
            val file = FileSaveManager.savePngToCache(context, bitmap, fileName)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Intent.createChooser(shareIntent, "Share encoded image")
        }
    }

    suspend fun createEmailIntent(context: Context, bitmap: Bitmap, fileName: String): Intent {
        return withContext(Dispatchers.IO) {
            val file = FileSaveManager.savePngToCache(context, bitmap, fileName)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_SUBJECT, "GhostMask encoded PNG")
                putExtra(Intent.EXTRA_TEXT, "Share this file as PNG only. Recompression may destroy the hidden payload.")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}
