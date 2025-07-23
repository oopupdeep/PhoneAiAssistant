package com.wang.phoneaiassistant.data.repository

import com.wang.phoneaiassistant.data.database.MessageEmbeddingDao
import com.wang.phoneaiassistant.data.database.MessageEmbeddingEntity
import com.wang.phoneaiassistant.data.database.MessageDao
import com.wang.phoneaiassistant.data.database.MessageEntity
import com.wang.phoneaiassistant.data.embeddings.EmbeddingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddingRepository @Inject constructor(
    private val messageEmbeddingDao: MessageEmbeddingDao,
    private val messageDao: MessageDao,
    private val embeddingService: EmbeddingService
) {
    
    data class SimilarMessage(
        val message: MessageEntity,
        val similarity: Float
    )
    
    suspend fun generateAndStoreEmbedding(messageId: String, content: String): MessageEmbeddingEntity {
        return withContext(Dispatchers.IO) {
            android.util.Log.d("EmbeddingRepository", "Generating embedding for message: $messageId")
            
            // Update document frequency for better TF-IDF calculations
            val tokens = content.lowercase()
                .replace(Regex("[^a-z0-9\\u4e00-\\u9fa5\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
            embeddingService.updateDocumentFrequency(tokens)
            
            val embedding = embeddingService.generateEmbedding(content)
            val embeddingEntity = MessageEmbeddingEntity(
                id = UUID.randomUUID().toString(),
                messageId = messageId,
                embedding = embedding,
                createdAt = Date()
            )
            
            messageEmbeddingDao.insertEmbedding(embeddingEntity)
            android.util.Log.d("EmbeddingRepository", "Stored embedding for message: $messageId")
            embeddingEntity
        }
    }
    
    suspend fun findSimilarMessages(queryText: String, topK: Int = 8, currentConversationId: String? = null): List<SimilarMessage> {
        return withContext(Dispatchers.IO) {
            android.util.Log.d("EmbeddingRepository", "Finding similar messages for: $queryText")
            val queryEmbedding = embeddingService.generateEmbedding(queryText)
            val recentEmbeddings = messageEmbeddingDao.getRecentEmbeddings(1000)
            android.util.Log.d("EmbeddingRepository", "Found ${recentEmbeddings.size} embeddings in database")
            
            val similarities = mutableListOf<SimilarMessage>()
            
            for (embeddingEntity in recentEmbeddings) {
                // Get the message directly by ID
                val message = messageDao.getMessageById(embeddingEntity.messageId)
                
                if (message != null) {
                    // Skip messages from current conversation to avoid circular reference
                    if (currentConversationId != null && message.conversationId == currentConversationId) {
                        continue
                    }
                    
                    val similarity = embeddingService.calculateCosineSimilarity(queryEmbedding, embeddingEntity.embedding)
                    similarities.add(SimilarMessage(message, similarity))
                }
            }
            
            // Sort by similarity and return top K
            similarities.sortedByDescending { it.similarity }.take(topK)
        }
    }
    
    suspend fun getEmbeddingForMessage(messageId: String): MessageEmbeddingEntity? {
        return withContext(Dispatchers.IO) {
            messageEmbeddingDao.getEmbeddingForMessage(messageId)
        }
    }
    
    suspend fun processRecentMessages(limit: Int = 1000) {
        withContext(Dispatchers.IO) {
            android.util.Log.d("EmbeddingRepository", "Processing recent messages for embeddings...")
            // Get recent messages across all conversations
            val recentMessages = messageDao.getRecentMessages(limit)
            android.util.Log.d("EmbeddingRepository", "Found ${recentMessages.size} recent messages")
            
            var processedCount = 0
            // Process messages that don't have embeddings yet
            for (message in recentMessages) {
                // Skip system messages
                if (message.role == "system") continue
                
                val existingEmbedding = messageEmbeddingDao.getEmbeddingForMessage(message.id)
                if (existingEmbedding == null) {
                    generateAndStoreEmbedding(message.id, message.content)
                    processedCount++
                }
            }
            android.util.Log.d("EmbeddingRepository", "Processed $processedCount new embeddings")
        }
    }
    
    suspend fun deleteEmbeddingForMessage(messageId: String) {
        withContext(Dispatchers.IO) {
            messageEmbeddingDao.deleteEmbeddingForMessage(messageId)
        }
    }
    
    suspend fun deleteEmbeddingsForConversation(conversationId: String) {
        withContext(Dispatchers.IO) {
            messageEmbeddingDao.deleteEmbeddingsForConversation(conversationId)
        }
    }
}