package com.wang.phoneaiassistant.data.entity.chat

import java.util.UUID

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "新的对话", // 默认标题
    val messages: MutableList<Message> = mutableListOf()
)