package com.liveasadodo.conversationgenerator.util

object InputValidator {
    private const val SITUATION_MAX_LENGTH = 500
    private const val SITUATION_MIN_LENGTH = 3
    private const val KEY_SENTENCE_MAX_LENGTH = 200
    private const val CONVERSATION_LENGTH_MIN = 2
    private const val CONVERSATION_LENGTH_MAX = 5

    fun validateSituation(input: String): ValidationResult {
        val trimmed = input.trim()

        return when {
            trimmed.isEmpty() -> ValidationResult.Error("Please enter a situation")
            trimmed.length < SITUATION_MIN_LENGTH -> ValidationResult.Error("Situation too short")
            trimmed.length > SITUATION_MAX_LENGTH -> ValidationResult.Error("Situation too long (max $SITUATION_MAX_LENGTH characters)")
            containsInappropriateContent(trimmed) -> ValidationResult.Error("Inappropriate content detected")
            else -> ValidationResult.Valid(trimmed)
        }
    }

    fun validateKeySentence(input: String?): ValidationResult {
        // Key sentence is optional, so null or empty is valid
        if (input.isNullOrBlank()) {
            return ValidationResult.Valid("")
        }

        val trimmed = input.trim()

        return when {
            trimmed.length > KEY_SENTENCE_MAX_LENGTH -> ValidationResult.Error("Key sentence too long (max $KEY_SENTENCE_MAX_LENGTH characters)")
            containsInappropriateContent(trimmed) -> ValidationResult.Error("Inappropriate content detected")
            else -> ValidationResult.Valid(trimmed)
        }
    }

    fun validateConversationLength(length: Int): ValidationResult {
        return when {
            length < CONVERSATION_LENGTH_MIN -> ValidationResult.Error("Conversation length must be at least $CONVERSATION_LENGTH_MIN turns")
            length > CONVERSATION_LENGTH_MAX -> ValidationResult.Error("Conversation length must be at most $CONVERSATION_LENGTH_MAX turns")
            else -> ValidationResult.Valid(length.toString())
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun containsInappropriateContent(input: String): Boolean {
        // Implement content filtering logic if needed
        // This is a placeholder - implement actual filtering based on requirements
        return false
    }
}

sealed class ValidationResult {
    data class Valid(val input: String) : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
