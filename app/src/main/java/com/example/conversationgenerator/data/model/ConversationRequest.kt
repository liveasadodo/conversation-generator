package com.example.conversationgenerator.data.model

data class ConversationRequest(
    val situation: String,
    val difficulty: String = "intermediate", // beginner, intermediate, advanced
    val length: String = "medium", // short (1-2 turns), medium (2-3 turns), long (4-5 turns)
    val includeTranslation: Boolean = false
)
