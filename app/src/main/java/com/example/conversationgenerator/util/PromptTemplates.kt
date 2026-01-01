package com.example.conversationgenerator.util

import com.example.conversationgenerator.data.model.Language

object PromptTemplates {
    fun generateConversationPrompt(
        situation: String,
        generationLanguage: Language = Language.ENGLISH,
        interfaceLanguage: Language? = null,
        keySentence: String? = null
    ): String {
        val languageName = generationLanguage.displayName
        val includeTranslation = interfaceLanguage != null && interfaceLanguage != generationLanguage
        val hasKeySentence = !keySentence.isNullOrBlank()

        return when {
            includeTranslation && hasKeySentence -> {
                """
                    Please generate a natural $languageName conversation suitable for the following situation. The conversation MUST naturally include the following key sentence.

                    Requirements:
                    - Conversation should consist of 2-3 exchanges
                    - Use practical and natural expressions
                    - Clearly distinguish each speaker's dialogue
                    - Include appropriate greetings and closing
                    - IMPORTANT: The key sentence must appear naturally within one of the speaker's dialogues

                    Situation: $situation
                    Key Sentence: $keySentence

                    Format (IMPORTANT - Follow this exact format):
                    **Title**

                    Speaker A: [original $languageName sentence]
                    [TRANSLATION]: [${interfaceLanguage!!.displayName} translation]

                    Speaker B: [original $languageName sentence]
                    [TRANSLATION]: [${interfaceLanguage.displayName} translation]

                    CRITICAL: Each speaker's line must be followed immediately by a line starting with "[TRANSLATION]:" containing the ${interfaceLanguage.displayName} translation.
                """.trimIndent()
            }
            includeTranslation -> {
                """
                    Please generate a natural $languageName conversation suitable for the following situation.

                    Requirements:
                    - Conversation should consist of 2-3 exchanges
                    - Use practical and natural expressions
                    - Clearly distinguish each speaker's dialogue
                    - Include appropriate greetings and closing

                    Situation: $situation

                    Format (IMPORTANT - Follow this exact format):
                    **Title**

                    Speaker A: [original $languageName sentence]
                    [TRANSLATION]: [${interfaceLanguage!!.displayName} translation]

                    Speaker B: [original $languageName sentence]
                    [TRANSLATION]: [${interfaceLanguage.displayName} translation]

                    CRITICAL: Each speaker's line must be followed immediately by a line starting with "[TRANSLATION]:" containing the ${interfaceLanguage.displayName} translation.
                """.trimIndent()
            }
            hasKeySentence -> {
                """
                    Please generate a natural $languageName conversation suitable for the following situation. The conversation MUST naturally include the following key sentence.

                    Requirements:
                    - Conversation should consist of 2-3 exchanges
                    - Use practical and natural expressions
                    - Clearly distinguish each speaker's dialogue
                    - Include appropriate greetings and closing
                    - IMPORTANT: The key sentence must appear naturally within one of the speaker's dialogues

                    Situation: $situation
                    Key Sentence: $keySentence

                    Format:
                    **Title**

                    Speaker A: ...
                    Speaker B: ...
                """.trimIndent()
            }
            else -> {
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
