package com.wang.phoneaiassistant.data.network


data class ChatRequest(
    val model: String,
    val messages: List<MessageBody>
)

data class MessageBody(
    val role: String,
    val content: String
)
