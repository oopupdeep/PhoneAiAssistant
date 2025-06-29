package com.wang.phoneaiassistant.data.entity.chat

import java.util.UUID

data class Message(
    val role: String,
    val content: String,
    val id: String = UUID.randomUUID().toString()
)