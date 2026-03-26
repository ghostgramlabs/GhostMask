package com.ghostgramlabs.ghostmask.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Manages saving encoded PNG images to device storage and cache.
 */
object FileSaveManager {

    data class PrivateEncodedFile(
        val file: File,
        val uri: Uri
    )

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

    suspend fun savePngToPrivateStorage(context: Context, bitmap: Bitmap, fileName: String): PrivateEncodedFile {
        return withContext(Dispatchers.IO) {
            val sanitizedName = sanitizeOutputName(fileName)
            val encodedDir = File(context.filesDir, "encoded").apply { mkdirs() }
            val file = File(encodedDir, sanitizedName)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            PrivateEncodedFile(file = file, uri = getContentUriForFile(context, file))
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

    fun listPrivateEncodedFiles(context: Context): List<PrivateEncodedFile> {
        val encodedDir = File(context.filesDir, "encoded")
        return encodedDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { PrivateEncodedFile(it, getContentUriForFile(context, it)) }
            .orEmpty()
    }

    fun createOpenFileIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun sanitizeOutputName(input: String): String {
        val trimmed = input.trim().ifBlank { "ghostmask_${System.currentTimeMillis()}" }
        val safe = trimmed.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_')
        val normalized = if (safe.isBlank()) "ghostmask_${System.currentTimeMillis()}" else safe
        return if (normalized.endsWith(".png", ignoreCase = true)) normalized else "$normalized.png"
    }

    private fun getContentUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
