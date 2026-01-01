package com.example.conversationgenerator.data.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): LiveData<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteConversations(): LiveData<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int

    @Query("SELECT COUNT(*) FROM conversations WHERE isFavorite = 0")
    suspend fun getNonFavoriteCount(): Int

    @Query("DELETE FROM conversations WHERE isFavorite = 0 AND id IN (SELECT id FROM conversations WHERE isFavorite = 0 ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestNonFavorites(count: Int)

    @Query("SELECT * FROM conversations WHERE isFavorite = 0 ORDER BY timestamp ASC LIMIT :count")
    suspend fun getOldestNonFavorites(count: Int): List<ConversationEntity>
}
