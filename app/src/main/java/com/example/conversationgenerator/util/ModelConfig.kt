package com.example.conversationgenerator.util

object ModelConfig {
    fun getModelName(buildType: String): String {
        return when (buildType) {
            "debug" -> "gemini-1.5-flash"  // Fast and free for development
            "release" -> "gemini-1.5-flash"  // Fast and free for production
            // Alternative: "gemini-1.5-pro" for higher quality if needed
            else -> "gemini-1.5-flash"
        }
    }

    const val MAX_OUTPUT_TOKENS = 1024
    const val TEMPERATURE = 0.7
    const val TOP_P = 0.95
    const val TOP_K = 40
}
