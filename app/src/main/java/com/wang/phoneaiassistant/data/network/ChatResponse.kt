package com.wang.phoneaiassistant.data.network

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: MessageBody
)