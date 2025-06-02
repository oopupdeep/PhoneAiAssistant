package com.wang.phoneaiassistant.data.network

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.Call

interface AiApiService {

    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")  // 以 OpenAI 为例，注意替换成你服务的路径
    fun chat(
        @Body request: ChatRequest
    ): Call<ChatResponse>
}
