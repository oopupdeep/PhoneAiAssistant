package com.wang.phoneaiassistant.data.entity.network

import com.google.gson.annotations.SerializedName
import com.wang.phoneaiassistant.data.entity.chat.Message

data class ChatResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("choices")
    val choices: List<Content>?
)

data class Content(
    @SerializedName("index")
    val index: Int,
    @SerializedName("message")
    val message: Message,
    @SerializedName("logprobs")
    val logprobs: Int?,
    @SerializedName("finish_reason")
    val finishReason: String
)