package com.wang.phoneaiassistant.data.entity.network

import com.wang.phoneaiassistant.data.entity.chat.Message

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean
)
