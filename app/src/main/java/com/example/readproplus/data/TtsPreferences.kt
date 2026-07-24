package com.example.readproplus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ttsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tts_settings")

class TtsPreferences(context: Context) {

    private val dataStore = context.ttsDataStore

    companion object {
        val KEY_SPEED = floatPreferencesKey("tts_speed")
        val KEY_VOLUME = floatPreferencesKey("tts_volume")
        val KEY_VOICE_ID = stringPreferencesKey("tts_voice_id")
        val KEY_MODEL_DOWNLOADED = booleanPreferencesKey("tts_model_downloaded")
        val KEY_AUTO_ADVANCE_PAGE = booleanPreferencesKey("tts_auto_advance")
    }

    val speed: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_SPEED] ?: 1.0f
    }

    val volume: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_VOLUME] ?: 1.0f
    }

    val voiceId: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_VOICE_ID] ?: "af_nicole"
    }

    val modelDownloaded: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_MODEL_DOWNLOADED] ?: false
    }

    val autoAdvancePage: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_ADVANCE_PAGE] ?: true
    }

    suspend fun setSpeed(speed: Float) {
        dataStore.edit { it[KEY_SPEED] = speed.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setVolume(volume: Float) {
        dataStore.edit { it[KEY_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setVoiceId(voiceId: String) {
        dataStore.edit { it[KEY_VOICE_ID] = voiceId }
    }

    suspend fun setModelDownloaded(downloaded: Boolean) {
        dataStore.edit { it[KEY_MODEL_DOWNLOADED] = downloaded }
    }

    suspend fun setAutoAdvancePage(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_ADVANCE_PAGE] = enabled }
    }
}
