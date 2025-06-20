package com.wang.phoneaiassistant.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wang.phoneaiassistant.data.network.entity.Message
import com.wang.phoneaiassistant.data.network.entity.ModelInfo
import com.wang.phoneaiassistant.ui.ShimmerText
// ✨ 确保导入你的新主题 ✨
import com.wang.phoneaiassistant.ui.theme.PhoneAiAssistantTheme
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val messages = viewModel.messages
    val input by viewModel.inputText
    val selectedModel by viewModel.selectedModel

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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
                            style = MaterialTheme.typography.titleMedium,
                            // ✨ 使用主题中的主要文字颜色 ✨
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        DropdownMenuButton(selectedModel) {
                            viewModel.onModelSelected(it)
                        }
                    }
                },
                // ✨ 1. TopAppBar样式更新：极致简约 ✨
                // 移除了原有的 containerColor，使其与背景融为一体，更显简约
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background, // 与主背景色相同
                    titleContentColor = MaterialTheme.colorScheme.onBackground // 标题颜色使用背景之上的颜色
                )
            )
        },
        bottomBar = {
            val topRoundedCornerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

            // ✨ 2. 底部输入栏样式更新：扁平化、线条化 ✨
            // 移除了 tonalElevation，使用顶部边框线来做视觉分割，更符合现代设计
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // 使用背景色，避免 Surface 默认的颜色叠加
                    .background(MaterialTheme.colorScheme.background)
                    // 在顶部添加一条细线作为分割
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ✨ 3. 输入框样式更新：由主题驱动 ✨
                // 移除了手动设置的 shadow，TextField 会自动从主题的`inputDecorationTheme`获取样式
                TextField(
                    value = input,
                    onValueChange = { viewModel.onInputChange(it) },
                    placeholder = { Text("输入你的问题...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    // 让 TextField 的颜色完全由主题控制，实现圆角、聚焦边框等效果
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.sendMessageStream()
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
                        // ✨ 4. 图标颜色更新：使用主题色 ✨
                        // 激活状态使用主题强调色，禁用状态使用主题中定义的辅助文字颜色，而非硬编码的 Color.Gray
                        tint = if (input.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                // ✨ 背景色将自动从主题获取，无需改动 ✨
                .background(MaterialTheme.colorScheme.background)
        ) {
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
}

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.role == "user"

    // ✨ 5. 聊天气泡颜色更新：语义化配色 ✨
    // 用户的消息使用醒目的主题色（光棱蓝），AI 的消息使用柔和的表面色（淡云灰）
    // 这比之前的 primaryContainer/secondaryContainer 对比更强烈，更具“科技感”
//    val bubbleColor =
//        if (isUser) MaterialTheme.colorScheme.primary
//        else MaterialTheme.colorScheme.surface
    val bubbleColor = MaterialTheme.colorScheme.surface
    // ✨ 6. 聊天气泡文字颜色更新：保证对比度 ✨
    // 根据气泡背景色，自动选择最合适的文字颜色（蓝底白字，灰底黑字）
//    val textColor =
//        if (isUser) MaterialTheme.colorScheme.onPrimary
//        else MaterialTheme.colorScheme.onSurface
    val textColor = MaterialTheme.colorScheme.onSurface
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

            if (isLoading) {
                ShimmerText(
                    text = message.content,
                    modifier = Modifier.padding(12.dp)
                )
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

@Composable
fun DropdownMenuButton(
    currentModel: ModelInfo,
    viewModel: ChatViewModel = viewModel(),
    onModelSelected: (ModelInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val models by viewModel.models.observeAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.loadModels()
    }

    Box {
        // ✨ 下拉按钮样式会由主题自动适配，无需修改 ✨
        OutlinedButton(
            onClick = { expanded = true }
        ) {
            Text(currentModel.id)
        }

        // ✨ 下拉菜单背景会自动使用 Surface 颜色，无需修改 ✨
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model.id) },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    // ✨ 在预览中也应用你的新主题，才能看到正确的效果 ✨
    PhoneAiAssistantTheme {
        ChatScreen()
    }
}