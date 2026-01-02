package com.liveasadodo.conversationgenerator.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val situation: String,
    val keySentence: String?,
    val conversationText: String,
    val generationLanguage: String,
    val interfaceLanguage: String?,
    val formality: String = "CASUAL",
    val conversationLength: Int = 3,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
