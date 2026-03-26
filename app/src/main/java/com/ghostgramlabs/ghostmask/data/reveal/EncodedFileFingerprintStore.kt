package com.ghostgramlabs.ghostmask.data.reveal

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.revealDataStore by preferencesDataStore(name = "ghostmask_reveal_prefs")

class EncodedFileFingerprintStore(private val context: Context) {

    suspend fun hasBeenRevealed(fingerprint: String): Boolean {
        return context.revealDataStore.data.first()[KEY_REVEALED_FINGERPRINTS]?.contains(fingerprint) == true
    }

    suspend fun markRevealed(fingerprint: String) {
        context.revealDataStore.edit { preferences ->
            val current = preferences[KEY_REVEALED_FINGERPRINTS].orEmpty().toMutableSet()
            current += fingerprint
            preferences[KEY_REVEALED_FINGERPRINTS] = current
        }
    }

    companion object {
        private val KEY_REVEALED_FINGERPRINTS: Preferences.Key<Set<String>> =
            stringSetPreferencesKey("revealed_fingerprints")
    }
}
