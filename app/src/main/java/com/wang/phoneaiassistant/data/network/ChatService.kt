package com.wang.phoneaiassistant.data.network

import com.wang.phoneaiassistant.data.entity.network.ChatRequest
import com.wang.phoneaiassistant.data.entity.network.ChatResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

interface ChatService {

    /**
     * 原有的非流式请求方法，可以保留以备他用。
     */
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun chat(
        @Body request: ChatRequest
    ): ChatResponse

    /**
     * ✨ 新增：用于流式输出的方法 ✨
     *
     * 1. @Streaming: 关键注解，告诉Retrofit不要一次性将整个响应加载到内存中。
     * 2. getChatStream: 一个新的方法名，用于区分非流式请求。
     * 3. ResponseBody: 返回类型。这是来自OkHttp的类，它让我们能以字节流的形式访问响应体。
     */
    @Streaming
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun getChatStream(
        @Body request: ChatRequest
    ): ResponseBody
}