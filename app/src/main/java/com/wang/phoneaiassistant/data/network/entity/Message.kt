package com.wang.phoneaiassistant.data.network.entity

import java.util.UUID

data class Message(
    val role: String,
    val content: String,
    val id: String = UUID.randomUUID().toString()
)