package com.wang.phoneaiassistant.ui.chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import com.wang.phoneaiassistant.ui.LatexView
import com.wang.phoneaiassistant.ui.ShimmerText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val messages = viewModel.messages
    val input by viewModel.inputText
    val selectedModel by viewModel.selectedModel

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // ✨ 关键改动：优化自动滚动逻辑 ✨
    // 当消息列表的大小或最后一条消息的内容发生变化时，自动滚动到底部
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "模型：${selectedModel.id}",
                            style = MaterialTheme.typography.titleMedium
                        )
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
                            viewModel.sendMessageStream()
                            // 点击发送时也触发一次滚动，确保用户消息可见
                            scope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.lastIndex)
                                }
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
                    .fillMaxSize()
            ) {
                // 使用 key 来帮助Compose优化性能
                items(messages, key = { it.id }) { msg ->
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
            // ✨ 核心改动在这里 ✨
            // 判断消息内容是否是加载中的占位符
            val isLoading =
                (message.content == ChatViewModel.LOADING_MESSAGE_CONTENT && message.role == "assistant")

            if (isLoading) {
                // 如果是，使用我们创建的 ShimmerText
                ShimmerText(
                    text = message.content,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
//                LatexView(
//                    latex = message.content,
//                    modifier = Modifier
//                        .padding(12.dp)
//                        .fillMaxWidth() // 让 WebView 有足够的空间渲染
//                )
                // 如果不是，使用原来的普通 Text
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
}

@Composable
fun DropdownMenuButton(
    currentModel: ModelInfo,
    viewModel: ChatViewModel = viewModel(),
    onModelSelected: (ModelInfo) -> Unit
) {
    // 1. 使用 remember 管理菜单的展开/折叠状态
    var expanded by remember { mutableStateOf(false) }

    // 2. 观察 ViewModel 中的 models LiveData (或 StateFlow)
    //    当 LiveData 更新时，这个 Composable 会自动重组，models 会获得新值
    val models by viewModel.models.observeAsState(initial = emptyList())

    // 3. 使用 LaunchedEffect 在 Composable 首次进入屏幕时执行副作用
    //    key1 = Unit 保证这个效应只执行一次，是加载初始数据的理想位置
    LaunchedEffect(Unit) {
        // 调用 ViewModel 的方法来加载模型列表
        viewModel.loadModels()
    }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(currentModel.id)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            // 4. 直接遍历从 observeAsState 获得的 models 状态
            //    这个列表是响应式的，会自动反映 ViewModel 中的最新数据
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model.id) },
                    onClick = {
                        onModelSelected(model)
                        expanded = false // 选择后关闭菜单
                    }
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    ChatScreen()
}
