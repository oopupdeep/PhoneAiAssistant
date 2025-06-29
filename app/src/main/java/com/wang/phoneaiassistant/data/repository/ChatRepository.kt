package com.wang.phoneaiassistant.data.repository

import android.util.Log
import com.wang.phoneaiassistant.data.network.ChatService
import com.wang.phoneaiassistant.data.entity.network.ChatRequest
import com.wang.phoneaiassistant.data.entity.network.ChatResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
            Log.e("ChatRepository", "Failed to get chat response", e)
            ChatResponse(id="errorMessage: "+e.message, type="null", role="null", choices=null)
        }
    }

    /**
     * ✨ 新增：用于获取流式响应的方法 ✨
     *
     * 1. 返回类型 Flow<String>: 这意味着方法会返回一个可以被异步收集的数据流，流中的每个元素都是一个字符串（即服务器返回的一行数据）。
     * 2. flow { ... }: 这是一个Flow构建器，我们在这里定义如何产生数据。
     * 3. chatService.getChatStream(chatRequest): 调用我们刚刚在Service中定义的新方法。
     * 4. reader.useLines: 这是一个高效读取文本行的方式，它能自动处理资源的关闭。
     * 5. emit(line): 将从服务器读取到的每一行数据发射出去，供ViewModel收集。
     * 6. flowOn(Dispatchers.IO): 确保所有的网络和文件读写操作都在后台的IO线程上执行，避免阻塞主线程。
     */
    fun getChatStream(chatRequest: ChatRequest): Flow<String> = flow {
        try {
            val responseBody = chatService.getChatStream(chatRequest)
            // 将响应体的字节流转换为可以逐行读取的BufferedReader
            val reader = responseBody.byteStream().bufferedReader()

            reader.useLines { lines ->
                lines.forEach { line ->
                    // 过滤掉空行
                    if (line.isNotBlank()) {
                        emit(line)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error in chat stream", e)
            // 当发生错误时，flow会停止并向上游（ViewModel）抛出异常。
            // ViewModel中的 .catch { ... } 操作符可以捕获并处理这个异常。
            throw e
        }
    }.flowOn(Dispatchers.IO)
}