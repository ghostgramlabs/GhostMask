package com.ghostgramlabs.ghostmask.data.reveal

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recentFilesDataStore by preferencesDataStore(name = "ghostmask_recent_files")

data class RecentEncodedFile(
    val uri: String,
    val addedAtEpochMs: Long
)

class RecentEncodedFilesStore(private val context: Context) {

    val recentFiles: Flow<List<RecentEncodedFile>> = context.recentFilesDataStore.data.map { prefs ->
        prefs.asMap()
            .mapNotNull { (key, value) ->
                if (!key.name.startsWith("$KEY_PREFIX:")) return@mapNotNull null
                val uri = key.name.removePrefix("$KEY_PREFIX:")
                val timestamp = (value as? String)?.toLongOrNull() ?: return@mapNotNull null
                RecentEncodedFile(uri = uri, addedAtEpochMs = timestamp)
            }
            .sortedByDescending { it.addedAtEpochMs }
            .take(MAX_RECENT_FILES)
    }

    suspend fun remember(uri: Uri) {
        val normalized = uri.toString()
        context.recentFilesDataStore.edit { prefs ->
            prefs[stringPreferencesKey("$KEY_PREFIX:$normalized")] = System.currentTimeMillis().toString()
            trimLocked(prefs)
        }
    }

    suspend fun clear() {
        context.recentFilesDataStore.edit { it.clear() }
    }

    private fun trimLocked(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        val all = prefs.asMap()
            .filterKeys { it.name.startsWith("$KEY_PREFIX:") }
            .mapNotNull { entry ->
                val time = (entry.value as? String)?.toLongOrNull() ?: return@mapNotNull null
                entry.key to time
            }
            .sortedByDescending { it.second }
        all.drop(MAX_RECENT_FILES).forEach { prefs.remove(it.first) }
    }

    companion object {
        private const val KEY_PREFIX = "recent_encoded"
        private const val MAX_RECENT_FILES = 12
    }
}
