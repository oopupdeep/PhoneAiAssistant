package com.wang.phoneaiassistant.data.repository

import com.wang.phoneaiassistant.data.database.ConversationDao
import com.wang.phoneaiassistant.data.database.ConversationEntity
import com.wang.phoneaiassistant.data.database.MessageDao
import com.wang.phoneaiassistant.data.database.MessageEntity
import com.wang.phoneaiassistant.data.entity.chat.Conversation
import com.wang.phoneaiassistant.data.entity.chat.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    
    fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { entity ->
                Conversation(
                    id = entity.id,
                    title = entity.title,
                    messages = mutableListOf()
                )
            }
        }
    }
    
    suspend fun getConversationWithMessages(id: String): Conversation? {
        val conversationEntity = conversationDao.getConversationById(id) ?: return null
        val messageEntities = messageDao.getMessagesForConversationSync(id)
        
        return Conversation(
            id = conversationEntity.id,
            title = conversationEntity.title,
            messages = messageEntities.map { messageEntity ->
                Message(
                    role = messageEntity.role,
                    content = messageEntity.content
                )
            }.toMutableList()
        )
    }
    
    suspend fun createConversation(title: String = "新的对话"): Conversation {
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            title = title,
            messages = mutableListOf(Message("system", "我是一名有用的AI助手"))
        )
        
        val now = Date()
        conversationDao.insertConversation(
            ConversationEntity(
                id = conversation.id,
                title = conversation.title,
                createdAt = now,
                updatedAt = now
            )
        )
        
        // 保存初始系统消息
        messageDao.insertMessage(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversation.id,
                role = "system",
                content = "我是一名有用的AI助手",
                timestamp = now
            )
        )
        
        return conversation
    }
    
    suspend fun saveMessage(conversationId: String, message: Message) {
        val messageEntity = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = message.role,
            content = message.content,
            timestamp = Date()
        )
        
        messageDao.insertMessage(messageEntity)
        
        // 更新对话的更新时间
        conversationDao.updateConversationTitle(
            conversationId,
            conversationDao.getConversationById(conversationId)?.title ?: "新的对话",
            Date()
        )
    }
    
    suspend fun updateConversationTitle(conversationId: String, newTitle: String) {
        conversationDao.updateConversationTitle(conversationId, newTitle, Date())
    }
    
    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteConversationById(conversationId)
    }
}