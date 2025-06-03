package com.wang.phoneaiassistant.data.network.entity

data class ChatResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: Content
)

data class Content(
    val type: String,
    val text: String,
    val annotations: List<String>
)