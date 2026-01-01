package com.example.conversationgenerator.data.repository

import androidx.lifecycle.LiveData
import com.example.conversationgenerator.data.database.ConversationDao
import com.example.conversationgenerator.data.database.ConversationEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConversationHistoryRepository(
    private val conversationDao: ConversationDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val MAX_HISTORY_SIZE = 30
    }

    val allConversations: LiveData<List<ConversationEntity>> = conversationDao.getAllConversations()
    val favoriteConversations: LiveData<List<ConversationEntity>> = conversationDao.getFavoriteConversations()

    suspend fun saveConversation(
        title: String,
        situation: String,
        keySentence: String?,
        conversationText: String,
        generationLanguage: String,
        interfaceLanguage: String?,
        formality: String = "CASUAL",
        conversationLength: Int = 3
    ): Long {
        return withContext(ioDispatcher) {
            val conversation = ConversationEntity(
                title = title,
                situation = situation,
                keySentence = keySentence,
                conversationText = conversationText,
                generationLanguage = generationLanguage,
                interfaceLanguage = interfaceLanguage,
                formality = formality,
                conversationLength = conversationLength
            )

            val conversationId = conversationDao.insertConversation(conversation)

            // Check if we need to clean up old conversations
            cleanupOldConversations()

            conversationId
        }
    }

    suspend fun toggleFavorite(conversation: ConversationEntity) {
        withContext(ioDispatcher) {
            conversationDao.updateConversation(
                conversation.copy(isFavorite = !conversation.isFavorite)
            )
        }
    }

    suspend fun deleteConversation(conversation: ConversationEntity) {
        withContext(ioDispatcher) {
            conversationDao.deleteConversation(conversation)
        }
    }

    suspend fun deleteConversationById(id: Long) {
        withContext(ioDispatcher) {
            conversationDao.deleteConversationById(id)
        }
    }

    private suspend fun cleanupOldConversations() {
        val nonFavoriteCount = conversationDao.getNonFavoriteCount()
        if (nonFavoriteCount > MAX_HISTORY_SIZE) {
            val deleteCount = nonFavoriteCount - MAX_HISTORY_SIZE
            conversationDao.deleteOldestNonFavorites(deleteCount)
        }
    }

    suspend fun getConversationById(id: Long): ConversationEntity? {
        return withContext(ioDispatcher) {
            conversationDao.getConversationById(id)
        }
    }
}
