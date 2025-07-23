package com.wang.phoneaiassistant.data.agent

import com.wang.phoneaiassistant.data.entity.chat.Message
import com.wang.phoneaiassistant.data.repository.EmbeddingRepository
import com.wang.phoneaiassistant.data.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextMemoryAgent @Inject constructor(
    private val embeddingRepository: EmbeddingRepository,
    private val conversationRepository: ConversationRepository
) {
    
    data class ContextualPrompt(
        val originalMessage: String,
        val enhancedPrompt: String,
        val contextSources: List<ContextSource>
    )
    
    data class ContextSource(
        val conversationId: String,
        val messageContent: String,
        val role: String,
        val similarity: Float,
        val timestamp: Long
    )
    
    companion object {
        private const val TOP_K_SIMILAR = 8
        private const val MIN_SIMILARITY_THRESHOLD = 0.1f  // Lowered for testing
        private const val MAX_CONTEXT_LENGTH = 2000
    }
    
    suspend fun enhancePromptWithContext(
        userMessage: String,
        currentConversationId: String
    ): ContextualPrompt {
        return withContext(Dispatchers.IO) {
            android.util.Log.d("ContextMemoryAgent", "Enhancing prompt for message: $userMessage")
            
            // Find similar messages from conversation history
            val similarMessages = embeddingRepository.findSimilarMessages(
                queryText = userMessage,
                topK = TOP_K_SIMILAR,
                currentConversationId = currentConversationId
            )
            
            android.util.Log.d("ContextMemoryAgent", "Found ${similarMessages.size} similar messages")
            similarMessages.forEach { 
                android.util.Log.d("ContextMemoryAgent", "Similar message: ${it.message.content.take(50)}... (similarity: ${it.similarity})")
            }
            
            // Filter by similarity threshold
            val relevantMessages = similarMessages.filter { 
                it.similarity >= MIN_SIMILARITY_THRESHOLD 
            }
            
            android.util.Log.d("ContextMemoryAgent", "After filtering: ${relevantMessages.size} relevant messages")
            
            if (relevantMessages.isEmpty()) {
                return@withContext ContextualPrompt(
                    originalMessage = userMessage,
                    enhancedPrompt = userMessage,
                    contextSources = emptyList()
                )
            }
            
            // Create context sources
            val contextSources = relevantMessages.map { similarMessage ->
                ContextSource(
                    conversationId = similarMessage.message.conversationId,
                    messageContent = similarMessage.message.content,
                    role = similarMessage.message.role,
                    similarity = similarMessage.similarity,
                    timestamp = similarMessage.message.timestamp.time
                )
            }
            
            // Build context string
            val contextString = buildContextString(contextSources)
            
            // Create enhanced prompt
            val enhancedPrompt = buildEnhancedPrompt(userMessage, contextString)
            
            ContextualPrompt(
                originalMessage = userMessage,
                enhancedPrompt = enhancedPrompt,
                contextSources = contextSources
            )
        }
    }
    
    private fun buildContextString(contextSources: List<ContextSource>): String {
        val contextBuilder = StringBuilder()
        
        var currentLength = 0
        for (source in contextSources) {
            val contextEntry = formatContextEntry(source)
            
            if (currentLength + contextEntry.length > MAX_CONTEXT_LENGTH) {
                break
            }
            
            contextBuilder.append(contextEntry)
            currentLength += contextEntry.length
        }
        
        return contextBuilder.toString()
    }
    
    private fun formatContextEntry(source: ContextSource): String {
        val role = if (source.role == "user") "用户" else "助手"
        return "[$role]: ${source.messageContent}\n"
    }
    
    private fun buildEnhancedPrompt(userMessage: String, contextString: String): String {
        return if (contextString.isNotBlank()) {
            """
            基于以下相关的历史对话上下文，请回答用户的问题：
            
            === 相关历史对话 ===
            $contextString
            
            === 当前问题 ===
            $userMessage
            
            请参考上述历史对话中的相关信息来回答问题，如果历史对话中没有相关信息，请基于你的知识正常回答。
            """.trimIndent()
        } else {
            userMessage
        }
    }
    
    suspend fun processNewMessage(message: Message) {
        withContext(Dispatchers.IO) {
            // Generate and store embedding for new message
            embeddingRepository.generateAndStoreEmbedding(
                messageId = message.id,
                content = message.content
            )
        }
    }
    
    suspend fun initializeEmbeddings() {
        withContext(Dispatchers.IO) {
            // Process recent messages to generate embeddings
            embeddingRepository.processRecentMessages(1000)
        }
    }
    
    suspend fun clearContextForConversation(conversationId: String) {
        withContext(Dispatchers.IO) {
            embeddingRepository.deleteEmbeddingsForConversation(conversationId)
        }
    }
}