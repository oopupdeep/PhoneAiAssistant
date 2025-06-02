package com.wang.phoneaiassistant.data.model

data class ModelInfo(
    val id: String,
    val name: String,
    val type: ModelType, // OPENAI, GEMINI, LOCAL
    val config: Map<String, String> // API key, path, endpoint 等
)