package com.example.conversationgenerator.util

import com.example.conversationgenerator.data.model.Language

object PromptTemplates {
    fun generateConversationPrompt(
        situation: String,
        generationLanguage: Language = Language.ENGLISH,
        interfaceLanguage: Language? = null
    ): String {
        val languageName = generationLanguage.displayName
        val includeTranslation = interfaceLanguage != null && interfaceLanguage != generationLanguage

        return if (includeTranslation) {
            """
                Please generate a natural $languageName conversation suitable for the following situation.

                Requirements:
                - Conversation should consist of 2-3 exchanges
                - Use practical and natural expressions
                - Clearly distinguish each speaker's dialogue
                - Include appropriate greetings and closing

                Situation: $situation

                Format:
                **Title**

                Speaker A: ... (in $languageName)
                (${interfaceLanguage!!.displayName} translation: ...)
                Speaker B: ... (in $languageName)
                (${interfaceLanguage.displayName} translation: ...)

                Provide the conversation in $languageName with ${interfaceLanguage.displayName} translations in parentheses after each line.
            """.trimIndent()
        } else {
            """
                Please generate a natural $languageName conversation suitable for the following situation.

                Requirements:
                - Conversation should consist of 2-3 exchanges
                - Use practical and natural expressions
                - Clearly distinguish each speaker's dialogue
                - Include appropriate greetings and closing

                Situation: $situation

                Format:
                **Title**

                Speaker A: ...
                Speaker B: ...
            """.trimIndent()
        }
    }

    fun generateWithDifficulty(situation: String, difficulty: String): String {
        val vocabularyLevel = when (difficulty) {
            "beginner" -> "Use simple, everyday vocabulary (A1-A2 level)"
            "intermediate" -> "Use common vocabulary and some idiomatic expressions (B1-B2 level)"
            "advanced" -> "Use sophisticated vocabulary and natural idioms (C1-C2 level)"
            else -> "Use natural, practical expressions"
        }

        return """
            Please generate a natural English conversation suitable for the following situation.

            Requirements:
            - Conversation should consist of 2-3 exchanges
            - $vocabularyLevel
            - Clearly distinguish each speaker's dialogue
            - Include appropriate greetings and closing

            Situation: $situation
        """.trimIndent()
    }

    fun generateWithLength(situation: String, length: String): String {
        val turnCount = when (length) {
            "short" -> "1-2 exchanges"
            "medium" -> "2-3 exchanges"
            "long" -> "4-5 exchanges"
            else -> "2-3 exchanges"
        }

        return """
            Please generate a natural English conversation suitable for the following situation.

            Requirements:
            - Conversation should consist of $turnCount
            - Use practical and natural expressions
            - Clearly distinguish each speaker's dialogue
            - Include appropriate greetings and closing

            Situation: $situation
        """.trimIndent()
    }
}
