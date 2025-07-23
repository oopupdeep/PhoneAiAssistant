package com.wang.phoneaiassistant.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wang.phoneaiassistant.data.entity.chat.Message
import com.wang.phoneaiassistant.ui.ShimmerText
import com.wang.phoneaiassistant.ui.chat.ChatViewModel
import com.wang.phoneaiassistant.ui.theme.PhoneAiAssistantTheme
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.util.UUID

@Composable
fun MessageBubble(
    message: Message,
    isStreaming: Boolean = false,
    onCancelStream: (() -> Unit)? = null
) {
    val isUser = message.role == "user"

    // ✨ 5. 聊天气泡颜色更新：语义化配色 ✨
    // 用户的消息使用醒目的主题色（光棱蓝），AI 的消息使用柔和的表面色（淡云灰）
    // 这比之前的 primaryContainer/secondaryContainer 对比更强烈，更具“科技感”
    val bubbleColor =
        if (isUser) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface
//    val bubbleColor = MaterialTheme.colorScheme.surface
    // ✨ 6. 聊天气泡文字颜色更新：保证对比度 ✨
    // 根据气泡背景色，自动选择最合适的文字颜色（蓝底白字，灰底黑字）
    val textColor =
        if (isUser) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface
//    val textColor = MaterialTheme.colorScheme.onSurface
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleShape = if (isUser) {
        MaterialTheme.shapes.medium.copy(bottomEnd = MaterialTheme.shapes.small.bottomStart)
    } else {
        MaterialTheme.shapes.medium.copy(bottomStart = MaterialTheme.shapes.small.bottomStart)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            // ✨ 7. 移除阴影：实现扁平化设计 ✨
            // 移除了 tonalElevation 和 shadow，让界面更干净、现代
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            val isLoading =
                (message.content == ChatViewModel.LOADING_MESSAGE_CONTENT && message.role == "assistant")

            if (isLoading || isStreaming) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        ShimmerText(
                            text = message.content,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        MarkdownText(
                            modifier = Modifier.weight(1f),
                            markdown = message.content,
                            style = TextStyle(
                                color = textColor,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Start
                            ),
                            onLinkClicked = { link ->
                                println("Link clicked: $link")
                            }
                        )
                    }
                    
                    // 终止按钮
                    if (onCancelStream != null) {
                        IconButton(
                            onClick = onCancelStream,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "终止生成",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                MarkdownText(
                    modifier = Modifier.padding(12.dp),
                    markdown = message.content,
                    // ✨ 将动态获取的文字颜色应用到 Markdown 文本 ✨
                    style = TextStyle(
                        color = textColor,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start
                    ),
                    onLinkClicked = { link ->
                        println("Link clicked: $link")
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun MessageBubblePreview() {
    PhoneAiAssistantTheme {
        Column {
            MessageBubble(
                message = Message(
                    id = UUID.randomUUID().toString(),
                    role = "user",
                    content = "你好，AI。这是一个用户的消息。"
                ),
                isStreaming = false,
                onCancelStream = null
            )
            Spacer(Modifier.height(8.dp))
            MessageBubble(
                message = Message(
                    id = UUID.randomUUID().toString(),
                    role = "assistant",
                    content = "你好！这是一个来自AI的回复。我能为你做些什么呢？"
                ),
                isStreaming = false,
                onCancelStream = null
            )
            Spacer(Modifier.height(8.dp))
            MessageBubble(
                message = Message(
                    id = UUID.randomUUID().toString(),
                    role = "assistant",
                    content = "这是一个正在生成的回复..."
                ),
                isStreaming = true,
                onCancelStream = { println("取消流式输出") }
            )
        }
    }
}