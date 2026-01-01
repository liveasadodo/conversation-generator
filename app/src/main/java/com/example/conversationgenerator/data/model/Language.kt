package com.example.conversationgenerator.data.model

import java.util.Locale

enum class Language(val displayName: String, val code: String, val flag: String) {
    ENGLISH("English", "en", "🇺🇸"),
    JAPANESE("日本語", "ja", "🇯🇵"),
    SPANISH("Español", "es", "🇪🇸"),
    FRENCH("Français", "fr", "🇫🇷"),
    GERMAN("Deutsch", "de", "🇩🇪"),
    CHINESE("中文", "zh", "🇨🇳"),
    KOREAN("한국어", "ko", "🇰🇷"),
    HINDI("हिन्दी", "hi", "🇮🇳");

    /**
     * Convert to Locale for TextToSpeech
     */
    fun toLocale(): Locale {
        return when (this) {
            ENGLISH -> Locale.ENGLISH
            JAPANESE -> Locale.JAPANESE
            SPANISH -> Locale("es")
            FRENCH -> Locale.FRENCH
            GERMAN -> Locale.GERMAN
            CHINESE -> Locale.CHINESE
            KOREAN -> Locale.KOREAN
            HINDI -> Locale("hi", "IN")
        }
    }

    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: ENGLISH
        }

        fun fromDisplayName(displayName: String): Language {
            return values().find { it.displayName == displayName } ?: ENGLISH
        }

        // Generation languages (all languages)
        fun getGenerationLanguages(): List<Language> {
            return values().toList()
        }

        // Interface languages (English and Japanese only)
        fun getInterfaceLanguages(): List<Language> {
            return listOf(ENGLISH, JAPANESE)
        }
    }
}
