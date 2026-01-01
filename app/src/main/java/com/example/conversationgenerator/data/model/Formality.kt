package com.example.conversationgenerator.data.model

/**
 * Formality levels for conversation generation
 */
enum class Formality(val displayNameEn: String, val displayNameJa: String) {
    FORMAL("Formal", "フォーマル"),
    BUSINESS_CASUAL("Business Casual", "ビジネスカジュアル"),
    CASUAL("Casual", "カジュアル"),
    BROKEN("Broken/Intimate", "ブロークン");

    /**
     * Get display name based on interface language
     */
    fun getDisplayName(language: Language): String {
        return when (language) {
            Language.JAPANESE -> displayNameJa
            else -> displayNameEn
        }
    }

    companion object {
        /**
         * Get formality from name string
         */
        fun fromName(name: String): Formality {
            return values().find { it.name == name } ?: CASUAL
        }

        /**
         * Get all formality levels
         */
        fun getAllFormalities(): List<Formality> {
            return values().toList()
        }
    }
}
