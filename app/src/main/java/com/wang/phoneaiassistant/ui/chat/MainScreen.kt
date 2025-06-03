package com.wang.phoneaiassistant.ui.chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wang.phoneaiassistant.data.preferences.AppPreferences
import com.wang.phoneaiassistant.data.network.entity.Message
import com.wang.phoneaiassistant.data.network.entity.ModelInfo
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val messages = viewModel.messages
    val input by viewModel.inputText
    val selectedModel by viewModel.selectedModel

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("模型：${selectedModel.name}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        DropdownMenuButton(selectedModel) {
                            viewModel.onModelSelected(it)
                        }
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = input,
                        onValueChange = { viewModel.onInputChange(it) },
                        placeholder = { Text("输入你的问题...") },
                        modifier = Modifier
                            .weight(1f)
                            .shadow(1.dp, MaterialTheme.shapes.small),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val appPrefs = AppPreferences(this as Context)
                            viewModel.sendMessage()
                            scope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        },
                        enabled = input.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "发送",
                            tint = if (input.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // 聊天消息区
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxSize(),
                reverseLayout = false // 最新消息在下方，滚动到底部
            ) {
                itemsIndexed(messages) { index, msg ->
                    // 当新消息到来时，让列表自动滚到底部
                    LaunchedEffect(messages.size) {
                        if (index == messages.lastIndex) {
                            listState.animateScrollToItem(index)
                        }
                    }
                    MessageBubble(msg)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    // 根据角色决定对齐、颜色与圆角
    val isUser = message.role == "user"
    val bubbleColor =
        if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    // 定义不同方向的圆角：用户消息右侧尖，助手消息左侧尖
    val bubbleShape = if (isUser) {
        // 用户：左上、左下、右上全部圆角，右下尖
        MaterialTheme.shapes.medium.copy(
            bottomEnd = MaterialTheme.shapes.small.bottomStart,
            bottomStart = MaterialTheme.shapes.medium.bottomStart,
            topStart = MaterialTheme.shapes.medium.topStart,
            topEnd = MaterialTheme.shapes.medium.topEnd
        )
    } else {
        // 助手：右上、右下、左上全部圆角，左下尖
        MaterialTheme.shapes.medium.copy(
            bottomStart = MaterialTheme.shapes.small.bottomStart,
            bottomEnd = MaterialTheme.shapes.medium.bottomEnd,
            topStart = MaterialTheme.shapes.medium.topStart,
            topEnd = MaterialTheme.shapes.medium.topEnd
        )
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
            tonalElevation = 2.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(1.dp, bubbleShape)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 16.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun DropdownMenuButton(currentModel: ModelInfo, onModelSelected: (ModelInfo) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(currentModel.name)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
        ) {
            mockModels.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model.name) },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    }
                )
            }
        }
    }
}



val mockModels = listOf(
    ModelInfo("gpt-4", "OpenAI GPT-4"),
    ModelInfo("gemini", "Gemini Pro"),
    ModelInfo("llama", "LLaMA (Local)")
)

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    ChatScreen()
}
