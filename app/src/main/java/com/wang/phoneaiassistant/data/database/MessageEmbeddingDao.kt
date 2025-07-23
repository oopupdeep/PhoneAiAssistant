package com.wang.phoneaiassistant.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageEmbeddingDao {
    @Query("SELECT * FROM message_embeddings WHERE messageId = :messageId")
    suspend fun getEmbeddingForMessage(messageId: String): MessageEmbeddingEntity?
    
    @Query("SELECT * FROM message_embeddings ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentEmbeddings(limit: Int): List<MessageEmbeddingEntity>
    
    @Query("""
        SELECT me.* FROM message_embeddings me
        INNER JOIN messages m ON me.messageId = m.id
        WHERE m.conversationId = :conversationId
        ORDER BY m.timestamp DESC
        LIMIT :limit
    """)
    suspend fun getEmbeddingsForConversation(conversationId: String, limit: Int): List<MessageEmbeddingEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: MessageEmbeddingEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbeddings(embeddings: List<MessageEmbeddingEntity>)
    
    @Delete
    suspend fun deleteEmbedding(embedding: MessageEmbeddingEntity)
    
    @Query("DELETE FROM message_embeddings WHERE messageId = :messageId")
    suspend fun deleteEmbeddingForMessage(messageId: String)
    
    @Query("DELETE FROM message_embeddings WHERE messageId IN (SELECT id FROM messages WHERE conversationId = :conversationId)")
    suspend fun deleteEmbeddingsForConversation(conversationId: String)
    
    @Query("SELECT COUNT(*) FROM message_embeddings")
    suspend fun getEmbeddingCount(): Int
}