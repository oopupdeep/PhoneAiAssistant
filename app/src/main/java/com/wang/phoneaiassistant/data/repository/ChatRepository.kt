package com.wang.phoneaiassistant.data.repository

import com.wang.phoneaiassistant.data.network.ChatService
import com.wang.phoneaiassistant.data.network.entity.ChatRequest
import com.wang.phoneaiassistant.data.network.entity.ChatResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val chatService: ChatService
) {

    suspend fun getChat(chatRequest: ChatRequest): ChatResponse = withContext(Dispatchers.IO) {
        try {
            val response = chatService.chat(chatRequest)
            response
        } catch (e: Exception) {
            ChatResponse()
        } as ChatResponse

    }
}