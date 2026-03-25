package com.ghostgramlabs.ghostmask.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Manages saving encoded PNG images to device storage and cache.
 */
object FileSaveManager {

    /**
     * Saves a bitmap as PNG to the Pictures/GhostMask directory using MediaStore.
     *
     * @return Content URI of the saved image
     */
    suspend fun savePngToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri {
        return withContext(Dispatchers.IO) {
            val name = if (fileName.endsWith(".png")) fileName else "$fileName.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/GhostMask")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Failed to create MediaStore entry")

            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                } ?: throw IllegalStateException("Failed to open output stream")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }

            uri
        }
    }

    /**
     * Saves a bitmap as PNG to the app's cache directory for sharing.
     *
     * @return File pointing to the cached PNG
     */
    suspend fun savePngToCache(context: Context, bitmap: Bitmap, fileName: String): File {
        return withContext(Dispatchers.IO) {
            val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(shareDir, if (fileName.endsWith(".png")) fileName else "$fileName.png")
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            file
        }
    }

    /**
     * Saves raw image bytes to the Pictures/GhostMask directory.
     * Used for saving revealed secret images.
     *
     * @return Content URI of the saved image
     */
    suspend fun saveImageBytesToGallery(
        context: Context,
        imageBytes: ByteArray,
        fileName: String,
        mimeType: String = "image/jpeg"
    ): Uri {
        return withContext(Dispatchers.IO) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/GhostMask")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Failed to create MediaStore entry")

            try {
                resolver.openOutputStream(uri)?.use { it.write(imageBytes) }
                    ?: throw IllegalStateException("Failed to open output stream")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }

            uri
        }
    }
}
