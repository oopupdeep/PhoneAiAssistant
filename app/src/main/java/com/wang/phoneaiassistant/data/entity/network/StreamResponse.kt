package com.wang.phoneaiassistant.data.entity.network

/**
 * 用于解析从SSE (Server-Sent Events) 流接收到的JSON数据块的顶层对象。
 * 例如: data: {"id":"...", "choices": [...]}
 */
data class StreamResponse(
    val id: String,
    val choices: List<StreamChoice>
)

/**
 * 代表 'choices' 数组中的一个元素。
 * 它包含了最关键的信息：delta。
 */
data class StreamChoice(
    val delta: StreamDelta,
    val finish_reason: String? // 流结束时，这里通常是 "stop"
)

/**
 * 代表实际的消息增量（变化量）。
 * 'content' 字段包含了我们需要的文本片段。
 * 'role' 字段通常只在第一个数据块中出现。
 */
data class StreamDelta(
    val role: String?,
    val content: String?
)