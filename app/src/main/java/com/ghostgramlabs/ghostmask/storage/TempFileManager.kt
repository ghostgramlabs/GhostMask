package com.ghostgramlabs.ghostmask.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class TempFileManager(private val context: Context) {

    suspend fun clearSensitiveCache() {
        withContext(Dispatchers.IO) {
            listOf(
                File(context.cacheDir, "shared"),
                File(context.cacheDir, "reveal")
            ).forEach { directory ->
                if (directory.exists()) {
                    directory.listFiles()?.forEach { it.delete() }
                }
            }
        }
    }
}
