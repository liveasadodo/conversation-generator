package com.example.conversationgenerator.util

object InputValidator {
    private const val MAX_LENGTH = 500
    private const val MIN_LENGTH = 3

    fun validateSituation(input: String): ValidationResult {
        val trimmed = input.trim()

        return when {
            trimmed.isEmpty() -> ValidationResult.Error("Please enter a situation")
            trimmed.length < MIN_LENGTH -> ValidationResult.Error("Situation too short")
            trimmed.length > MAX_LENGTH -> ValidationResult.Error("Situation too long (max $MAX_LENGTH characters)")
            containsInappropriateContent(trimmed) -> ValidationResult.Error("Inappropriate content detected")
            else -> ValidationResult.Valid(trimmed)
        }
    }

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
