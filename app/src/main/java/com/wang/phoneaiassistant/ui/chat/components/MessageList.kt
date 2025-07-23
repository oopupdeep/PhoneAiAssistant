package com.wang.phoneaiassistant.ui.chat.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.wang.phoneaiassistant.data.entity.chat.Message

@Composable
fun MessageList(
    messages: List<Message>,
    backgroundUri: String? = null,
    isStreaming: Boolean = false,
    onCancelStream: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // 调试日志
    LaunchedEffect(messages) {
        android.util.Log.d("MessageList", "Messages updated, count: ${messages.size}")
        messages.forEachIndexed { index, message ->
            android.util.Log.d("MessageList", "Message[$index]: role=${message.role}, content=${message.content.take(50)}...")
        }
    }

    // 当有新消息添加时（消息数量变化），平滑滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }
    
    // 监听最后一条消息的内容变化
    val lastMessageContent = messages.lastOrNull()?.content
    
    // 在流式输出时，监听布局信息变化并自动滚动
    LaunchedEffect(isStreaming, listState) {
        if (isStreaming) {
            snapshotFlow { listState.layoutInfo }
                .distinctUntilChanged { old, new ->
                    // 当可见项或其大小发生变化时触发
                    old.visibleItemsInfo.size == new.visibleItemsInfo.size &&
                    old.visibleItemsInfo.lastOrNull()?.size == new.visibleItemsInfo.lastOrNull()?.size &&
                    old.totalItemsCount == new.totalItemsCount
                }
                .collect { layoutInfo ->
                    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val totalItems = layoutInfo.totalItemsCount
                    
                    // 如果用户在底部附近，自动滚动到最底部
                    if (totalItems > 0 && lastVisibleIndex >= totalItems - 4) {
                        listState.scrollToItem(totalItems - 1)
                    }
                }
        }
    }
    
    // 当消息内容变化时立即滚动（用于快速响应）
    LaunchedEffect(lastMessageContent) {
        if (isStreaming && messages.isNotEmpty()) {
            // 立即滚动，不等待
            coroutineScope.launch {
                val totalItems = listState.layoutInfo.totalItemsCount
                if (totalItems > 0) {
                    listState.scrollToItem(totalItems - 1)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 背景层
        if (!backgroundUri.isNullOrEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = backgroundUri,
                    onError = { error ->
                        // 调试用
                        android.util.Log.e("MessageList", "Failed to load background image: ${error.result.throwable}")
                    }
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.8f  // 添加一些透明度使文字更易读
            )
        } else {
            // 没有背景图片时显示默认背景色
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize()
        ) {
            items(messages, key = { it.id }) { msg ->
                val isLastMessage = msg == messages.lastOrNull()
                val isAssistantMessage = msg.role == "assistant"
                val showCancelButton = isLastMessage && isAssistantMessage && isStreaming
                
                MessageBubble(
                    message = msg,
                    isStreaming = showCancelButton,
                    onCancelStream = if (showCancelButton) onCancelStream else null
                )
            }
            // 在列表底部留出更多空间，确保最后一条消息完全可见
            item(key = "bottom_spacer") { 
                Spacer(modifier = Modifier.height(
                    if (isStreaming) 120.dp else 8.dp
                )) 
            }
        }
    }
}