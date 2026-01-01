package com.example.conversationgenerator.util

import com.example.conversationgenerator.data.model.Formality
import com.example.conversationgenerator.data.model.Language

object PromptTemplates {

    /**
     * Get formality instruction based on formality level and language
     */
    fun getFormalityInstruction(formality: Formality, language: Language): String {
        return when (formality) {
            Formality.FORMAL -> when (language) {
                Language.JAPANESE -> "- Use formal, polite language (敬語、謙譲語、丁寧語) appropriate for business or official settings with proper honorifics"
                else -> "- Use formal, polite language appropriate for business or official settings with proper honorifics and respectful expressions"
            }
            Formality.BUSINESS_CASUAL -> when (language) {
                Language.JAPANESE -> "- Use professional but friendly language (丁寧語中心) suitable for colleagues, balancing politeness with approachability"
                else -> "- Use professional but friendly language suitable for colleagues, balancing politeness with approachability"
            }
            Formality.CASUAL -> when (language) {
                Language.JAPANESE -> "- Use everyday conversational language (普通の会話) between friends or acquaintances with natural, relaxed tone"
                else -> "- Use everyday conversational language between friends or acquaintances with natural, relaxed tone"
            }
            Formality.BROKEN -> when (language) {
                Language.JAPANESE -> "- Use very casual, intimate language (砕けた表現、若者言葉) between close friends or family with colloquialisms and slang"
                else -> "- Use very casual, intimate language between close friends or family with colloquialisms, slang, and informal expressions"
            }
        }
    }


    fun generateConversationPrompt(
        situation: String,
        generationLanguage: Language = Language.ENGLISH,
        interfaceLanguage: Language? = null,
        keySentence: String? = null,
        formality: Formality = Formality.CASUAL,
        conversationLength: Int = 3
    ): String {
        val languageName = generationLanguage.displayName
        val includeTranslation = interfaceLanguage != null && interfaceLanguage != generationLanguage
        val hasKeySentence = !keySentence.isNullOrBlank()
        val formalityInstruction = getFormalityInstruction(formality, generationLanguage)

        return when {
            includeTranslation && hasKeySentence -> {
                """
                    Please generate a natural $languageName conversation suitable for the following situation. The conversation MUST naturally include the following key sentence.

                    Requirements:
                    - Conversation should consist of exactly $conversationLength exchanges (turns)
                    - Note: $conversationLength exchanges means $conversationLength pairs of Speaker A and Speaker B dialogue
                    $formalityInstruction
                    - Clearly distinguish each speaker's dialogue
                    - Include appropriate greetings and closing
                    - IMPORTANT: The key sentence must appear naturally within one of the speaker's dialogues

                    Situation: $situation
                    Key Sentence: $keySentence

                    IMPORTANT: Respond with ONLY valid JSON (no markdown formatting, no code blocks). Use this exact structure:
                    {
                      "title": "Conversation title in $languageName",
                      "titleTranslation": "${interfaceLanguage!!.displayName} translation of title",
                      "lines": [
                        {
                          "speaker": "Speaker name in $languageName",
                          "speakerTranslation": "${interfaceLanguage.displayName} translation of speaker name",
                          "text": "Dialogue text in $languageName",
                          "translation": "${interfaceLanguage.displayName} translation of dialogue"
                        }
                      ]
                    }
                """.trimIndent()
            }
            includeTranslation -> {
                """
                    Please generate a natural $languageName conversation suitable for the following situation.

                    Requirements:
                    - Conversation should consist of exactly $conversationLength exchanges (turns)
                    - Note: $conversationLength exchanges means $conversationLength pairs of Speaker A and Speaker B dialogue
                    $formalityInstruction
                    - Clearly distinguish each speaker's dialogue
                    - Include appropriate greetings and closing

                    Situation: $situation

                    IMPORTANT: Respond with ONLY valid JSON (no markdown formatting, no code blocks). Use this exact structure:
                    {
                      "title": "Conversation title in $languageName",
                      "titleTranslation": "${interfaceLanguage!!.displayName} translation of title",
                      "lines": [
                        {
                          "speaker": "Speaker name in $languageName",
                          "speakerTranslation": "${interfaceLanguage.displayName} translation of speaker name",
                          "text": "Dialogue text in $languageName",
                          "translation": "${interfaceLanguage.displayName} translation of dialogue"
                        }
                      ]
                    }
                """.trimIndent()
            }
            hasKeySentence -> {
                """
                    Please generate a natural $languageName conversation suitable for the following situation. The conversation MUST naturally include the following key sentence.

                    Requirements:
                    - Conversation should consist of exactly $conversationLength exchanges (turns)
                    - Note: $conversationLength exchanges means $conversationLength pairs of Speaker A and Speaker B dialogue
                    $formalityInstruction
                    - Clearly distinguish each speaker's dialogue
                    - Include appropriate greetings and closing
                    - IMPORTANT: The key sentence must appear naturally within one of the speaker's dialogues

                    Situation: $situation
                    Key Sentence: $keySentence

                    IMPORTANT: Respond with ONLY valid JSON (no markdown formatting, no code blocks). Use this exact structure:
                    {
                      "title": "Conversation title in $languageName",
                      "titleTranslation": null,
                      "lines": [
                        {
                          "speaker": "Speaker name in $languageName",
                          "speakerTranslation": null,
                          "text": "Dialogue text in $languageName",
                          "translation": null
                        }
                      ]
                    }
                """.trimIndent()
            }
            else -> {
                """
                    Please generate a natural $languageName conversation suitable for the following situation.

                    Requirements:
                    - Conversation should consist of exactly $conversationLength exchanges (turns)
                    - Note: $conversationLength exchanges means $conversationLength pairs of Speaker A and Speaker B dialogue
                    $formalityInstruction
                    - Clearly distinguish each speaker's dialogue
                    - Include appropriate greetings and closing

                    Situation: $situation

                    IMPORTANT: Respond with ONLY valid JSON (no markdown formatting, no code blocks). Use this exact structure:
                    {
                      "title": "Conversation title in $languageName",
                      "titleTranslation": null,
                      "lines": [
                        {
                          "speaker": "Speaker name in $languageName",
                          "speakerTranslation": null,
                          "text": "Dialogue text in $languageName",
                          "translation": null
                        }
                      ]
                    }
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
