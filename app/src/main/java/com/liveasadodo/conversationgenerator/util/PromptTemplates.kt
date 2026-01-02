package com.liveasadodo.conversationgenerator.util

import com.liveasadodo.conversationgenerator.data.model.Formality
import com.liveasadodo.conversationgenerator.data.model.Language

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

    /**
     * Builds the introduction section of the prompt
     */
    private fun buildIntroduction(
        generationLanguage: Language,
        hasKeySentence: Boolean
    ): String {
        val generationLangName = generationLanguage.displayName
        return if (hasKeySentence) {
            "Please generate a natural $generationLangName conversation suitable for the following situation. The conversation MUST naturally include the following key sentence."
        } else {
            "Please generate a natural $generationLangName conversation suitable for the following situation."
        }
    }

    /**
     * Builds the requirements section of the prompt
     */
    private fun buildRequirements(
        conversationLength: Int,
        formalityInstruction: String,
        hasKeySentence: Boolean
    ): String {
        val baseRequirements = """
            Requirements:
            - Conversation should consist of exactly $conversationLength exchanges (turns)
            - Note: $conversationLength exchanges means $conversationLength pairs of Speaker A and Speaker B dialogue
            $formalityInstruction
        """.trimIndent()

        return if (hasKeySentence) {
            baseRequirements + "\n- IMPORTANT: The key sentence must appear naturally within one of the speaker's dialogues"
        } else {
            baseRequirements
        }
    }

    /**
     * Builds the situation section of the prompt
     */
    private fun buildSituation(
        situation: String,
        keySentence: String?
    ): String {
        return if (!keySentence.isNullOrBlank()) {
            """
                Situation: $situation
                Key Sentence: $keySentence
            """.trimIndent()
        } else {
            "Situation: $situation"
        }
    }

    /**
     * Builds the JSON structure specification
     */
    private fun buildJsonStructure(
        generationLanguage: Language,
        translationLanguage: Language?
    ): String {
        val generationLangName = generationLanguage.displayName
        return if (translationLanguage != null) {
            val translationLangName = translationLanguage.displayName
            """
                IMPORTANT: Respond with ONLY valid JSON (no markdown formatting, no code blocks). Use this exact structure:
                {
                  "title": "Conversation title in $generationLangName",
                  "titleTranslation": "$translationLangName translation of title",
                  "lines": [
                    {
                      "speaker": "Speaker name in $generationLangName",
                      "speakerTranslation": "$translationLangName translation of speaker name",
                      "text": "Dialogue text in $generationLangName",
                      "translation": "$translationLangName translation of dialogue"
                    }
                  ]
                }
            """.trimIndent()
        } else {
            """
                IMPORTANT: Respond with ONLY valid JSON (no markdown formatting, no code blocks). Use this exact structure:
                {
                  "title": "Conversation title in $generationLangName",
                  "titleTranslation": null,
                  "lines": [
                    {
                      "speaker": "Speaker name in $generationLangName",
                      "speakerTranslation": null,
                      "text": "Dialogue text in $generationLangName",
                      "translation": null
                    }
                  ]
                }
            """.trimIndent()
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
        val translationLanguage = if (interfaceLanguage != null && interfaceLanguage != generationLanguage) {
            interfaceLanguage
        } else {
            null
        }
        val hasKeySentence = !keySentence.isNullOrBlank()
        val formalityInstruction = getFormalityInstruction(formality, generationLanguage)

        val introduction = buildIntroduction(generationLanguage, hasKeySentence)
        val requirements = buildRequirements(conversationLength, formalityInstruction, hasKeySentence)
        val situationSection = buildSituation(situation, keySentence)
        val jsonStructure = buildJsonStructure(generationLanguage, translationLanguage)

        return """
            $introduction

            $requirements

            $situationSection

            $jsonStructure
        """.trimIndent()
    }
}
