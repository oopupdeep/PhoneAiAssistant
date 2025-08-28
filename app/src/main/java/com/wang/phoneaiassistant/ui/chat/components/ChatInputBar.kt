package com.wang.phoneaiassistant.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wang.phoneaiassistant.ui.theme.PhoneAiAssistantTheme

@Composable
fun ChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    contextMemoryEnabled: Boolean = true,
    onContextMemoryToggle: () -> Unit = {},
    isListening: Boolean = false,
    onVoiceInputClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 添加调试日志
    println("ChatInputBar recompose: contextMemoryEnabled=$contextMemoryEnabled")
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("输入你的问题...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        // 语音输入按钮
        IconButton(
            onClick = {
                println("ChatInputBar: Voice input button clicked")
                Toast.makeText(context, "语音输入按钮被点击", Toast.LENGTH_SHORT).show()
                println("ChatInputBar: About to call onVoiceInputClick")
                try {
                    onVoiceInputClick()
                    println("ChatInputBar: onVoiceInputClick called successfully")
                } catch (e: Exception) {
                    println("ChatInputBar: Error calling onVoiceInputClick: ${e.message}")
                    e.printStackTrace()
                }
            }
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "停止录音" else "开始录音",
                tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 上下文记忆开关按钮
        IconButton(
            onClick = {
                println("ChatInputBar: Context memory toggle clicked, current state: $contextMemoryEnabled")
                Toast.makeText(context, 
                    if (contextMemoryEnabled) "关闭上下文记忆" else "开启上下文记忆", 
                    Toast.LENGTH_SHORT
                ).show()
                onContextMemoryToggle()
            }
        ) {
            Icon(
                imageVector = if (contextMemoryEnabled) Icons.Filled.Psychology else Icons.Outlined.Psychology,
                contentDescription = if (contextMemoryEnabled) "关闭上下文记忆" else "开启上下文记忆",
                tint = if (contextMemoryEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onSendClick,
            enabled = input.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "发送",
                tint = if (input.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun ChatInputBarPreview() {
    PhoneAiAssistantTheme {
        ChatInputBar(
            input = "这是一个例子", 
            onInputChange = {}, 
            onSendClick = {},
            contextMemoryEnabled = true,
            onContextMemoryToggle = {}
        )
    }
}