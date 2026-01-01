package com.example.conversationgenerator.data.model

import com.example.conversationgenerator.R

/**
 * Formality levels for conversation generation
 */
enum class Formality(val stringResId: Int) {
    FORMAL(R.string.formality_formal),
    BUSINESS_CASUAL(R.string.formality_business_casual),
    CASUAL(R.string.formality_casual),
    BROKEN(R.string.formality_broken);

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
