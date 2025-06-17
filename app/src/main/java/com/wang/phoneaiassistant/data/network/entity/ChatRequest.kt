package com.wang.phoneaiassistant.data.network.entity

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean
)
