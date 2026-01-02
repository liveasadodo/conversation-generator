package com.liveasadodo.conversationgenerator.util

object ModelConfig {
    fun getModelName(buildType: String): String {
        return when (buildType) {
            "debug" -> "gemini-2.5-flash"
            "release" -> "gemini-2.5-flash"
            else -> "gemini-2.5-flash"
        }
    }

    const val MAX_OUTPUT_TOKENS = 1024
    const val TEMPERATURE = 0.7
    const val TOP_P = 0.95
    const val TOP_K = 40
}
