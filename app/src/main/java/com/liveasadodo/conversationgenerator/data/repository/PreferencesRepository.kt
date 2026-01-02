package com.liveasadodo.conversationgenerator.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.liveasadodo.conversationgenerator.data.model.Formality
import com.liveasadodo.conversationgenerator.data.model.Language

/**
 * Repository for managing application preferences.
 * Centralizes all SharedPreferences access and provides a clean API for preference management.
 */
class PreferencesRepository(context: Context) {

    private val apiKeysPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_API_KEYS, Context.MODE_PRIVATE)

    private val languagePrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_API_KEYS = "api_keys"
        private const val PREFS_LANGUAGE = "language_prefs"

        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GENERATION_LANGUAGE = "generation_language"
        private const val KEY_INTERFACE_LANGUAGE = "interface_language"
        private const val KEY_FORMALITY = "formality"
        private const val KEY_CONVERSATION_LENGTH = "conversation_length"

        private const val DEFAULT_CONVERSATION_LENGTH = 3
    }

    // API Key preferences

    fun getApiKey(): String? {
        return apiKeysPrefs.getString(KEY_GEMINI_API_KEY, null)
    }

    fun saveApiKey(apiKey: String) {
        apiKeysPrefs.edit()
            .putString(KEY_GEMINI_API_KEY, apiKey)
            .apply()
    }

    fun clearApiKey() {
        apiKeysPrefs.edit()
            .remove(KEY_GEMINI_API_KEY)
            .apply()
    }

    // Generation Language preferences

    fun getGenerationLanguage(): Language {
        val code = languagePrefs.getString(KEY_GENERATION_LANGUAGE, Language.ENGLISH.code)
        return Language.entries.find { it.code == code } ?: Language.ENGLISH
    }

    fun saveGenerationLanguage(language: Language) {
        languagePrefs.edit()
            .putString(KEY_GENERATION_LANGUAGE, language.code)
            .apply()
    }

    // Interface Language preferences

    fun getInterfaceLanguage(): Language {
        val code = languagePrefs.getString(KEY_INTERFACE_LANGUAGE, Language.JAPANESE.code)
        return Language.entries.find { it.code == code } ?: Language.JAPANESE
    }

    fun saveInterfaceLanguage(language: Language) {
        languagePrefs.edit()
            .putString(KEY_INTERFACE_LANGUAGE, language.code)
            .apply()
    }

    // Formality preferences

    fun getFormality(): Formality {
        val name = languagePrefs.getString(KEY_FORMALITY, Formality.CASUAL.name)
        return try {
            Formality.valueOf(name ?: Formality.CASUAL.name)
        } catch (e: IllegalArgumentException) {
            Formality.CASUAL
        }
    }

    fun saveFormality(formality: Formality) {
        languagePrefs.edit()
            .putString(KEY_FORMALITY, formality.name)
            .apply()
    }

    // Conversation Length preferences

    fun getConversationLength(): Int {
        return languagePrefs.getInt(KEY_CONVERSATION_LENGTH, DEFAULT_CONVERSATION_LENGTH)
    }

    fun saveConversationLength(length: Int) {
        languagePrefs.edit()
            .putInt(KEY_CONVERSATION_LENGTH, length)
            .apply()
    }

    /**
     * Get the interface language code for locale configuration.
     */
    fun getInterfaceLanguageCode(): String {
        return languagePrefs.getString(KEY_INTERFACE_LANGUAGE, Language.JAPANESE.code)
            ?: Language.JAPANESE.code
    }

    /**
     * Get the generation language code.
     */
    fun getGenerationLanguageCode(): String {
        return languagePrefs.getString(KEY_GENERATION_LANGUAGE, Language.ENGLISH.code)
            ?: Language.ENGLISH.code
    }
}
