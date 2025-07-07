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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.wang.phoneaiassistant.data.entity.chat.Message

@Composable
fun MessageList(
    messages: List<Message>,
    backgroundUri: String? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // 调试日志
    LaunchedEffect(messages) {
        android.util.Log.d("MessageList", "Messages updated, count: ${messages.size}")
        messages.forEachIndexed { index, message ->
            android.util.Log.d("MessageList", "Message[$index]: role=${message.role}, content=${message.content.take(50)}...")
        }
    }

    // 当消息列表更新时，自动滚动到底部
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
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
                MessageBubble(msg)
            }
            // 在列表底部留出一些空间
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}