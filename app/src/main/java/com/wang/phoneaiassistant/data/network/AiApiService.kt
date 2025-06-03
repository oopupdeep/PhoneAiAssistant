package com.wang.phoneaiassistant.data.network

import com.wang.phoneaiassistant.data.network.entity.ChatRequest
import com.wang.phoneaiassistant.data.network.entity.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.Call

interface AiApiService {

    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    fun chat(
        @Body request: ChatRequest
    ): Call<ChatResponse>
}
