package com.qinmomeak.recording

import android.content.Context
import android.net.Uri

class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun getAiApiKeyOverride(): String = prefs.getString(KEY_AI_API_KEY_OVERRIDE, "").orEmpty().trim()

    fun setAiApiKeyOverride(value: String) {
        prefs.edit().putString(KEY_AI_API_KEY_OVERRIDE, value.trim()).apply()
    }

    fun isSaveExtractedAudioEnabled(): Boolean = prefs.getBoolean(KEY_SAVE_EXTRACTED_AUDIO, false)

    fun setSaveExtractedAudioEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_EXTRACTED_AUDIO, enabled).apply()
    }

    fun getExtractedAudioTreeUri(): Uri? {
        val raw = prefs.getString(KEY_EXTRACTED_AUDIO_TREE_URI, "").orEmpty()
        if (raw.isBlank()) return null
        return runCatching { Uri.parse(raw) }.getOrNull()
    }

    fun setExtractedAudioTreeUri(uri: Uri?) {
        prefs.edit().putString(KEY_EXTRACTED_AUDIO_TREE_URI, uri?.toString().orEmpty()).apply()
    }

    companion object {
        private const val KEY_AI_API_KEY_OVERRIDE = "ai_api_key_override"
        private const val KEY_SAVE_EXTRACTED_AUDIO = "save_extracted_audio"
        private const val KEY_EXTRACTED_AUDIO_TREE_URI = "extracted_audio_tree_uri"
    }
}

