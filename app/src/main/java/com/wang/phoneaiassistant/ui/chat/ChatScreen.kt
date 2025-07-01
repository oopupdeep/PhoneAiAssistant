package com.wang.phoneaiassistant.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wang.phoneaiassistant.ui.chat.components.ChatInputBar
import com.wang.phoneaiassistant.ui.chat.components.ChatTopAppBar
import com.wang.phoneaiassistant.ui.chat.components.MessageList
import com.wang.phoneaiassistant.ui.chat.components.ModelDropdowns
import com.wang.phoneaiassistant.ui.chat.components.AppDrawer
import com.wang.phoneaiassistant.ui.theme.PhoneAiAssistantTheme
import kotlinx.coroutines.launch
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenNew(viewModel: ChatViewModel = hiltViewModel()) {
    // --- 状态管理 ---
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 从 ViewModel 中获取多对话相关状态
    val currentConversation by viewModel.currentConversation.collectAsState()
    val inputText by viewModel.inputText
    
    LaunchedEffect(currentConversation) {
        Log.d("ChatScreenNew", "currentConversation changed: ${currentConversation?.id}, messages: ${currentConversation?.messages?.size}")
    }

    // --- API Key 对话框所需的状态 ---
    val companyToSet by viewModel.showApiInputDialogForCompany.observeAsState()
    var tempApiKey by remember { mutableStateOf("") }
    val selectedCompany by viewModel.selectedCompany
    // 使用 remember a key 来确保在 selectedCompany 变化时，prevCompany能正确更新并被记住
    var prevCompany by remember(selectedCompany) { mutableStateOf(selectedCompany) }


    // --- API Key 对话框 UI ---
    // 这部分逻辑从原始代码中完整保留了下来
    if (companyToSet == true) { // 假设 showApiInputDialogForCompany 是一个 Boolean LiveData
        val companyName = selectedCompany
        AlertDialog(
            onDismissRequest = {
                viewModel.onApiKeyDialogDismissed(prevCompany)
            },
            title = { Text(text = "设置 API Key") },
            text = {
                Column {
                    Text(text = "请先为 $companyName 设置 API Key:")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("在此输入您的Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onApiKeySubmitted(companyName, tempApiKey)
                        prevCompany = companyName // 更新 prevCompany
                        tempApiKey = "" // 清空临时 key
                    },
                    enabled = tempApiKey.isNotBlank()
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onApiKeyDialogDismissed(prevCompany)
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // --- 整体UI结构 ---
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                onConversationClick = { conversationId ->
                    viewModel.switchChat(conversationId)
                    scope.launch { drawerState.close() }
                },
                onSettingsClick = {
                    // TODO: 在这里实现导航到设置页面的逻辑
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                ChatTopAppBar(
                    onNavigationIconClick = {
                        scope.launch { drawerState.open() }
                    },
                    onNewChatClick = {
                        viewModel.createNewChat()
                        // 如果抽屉是打开的，创建一个新对话后将其关闭
                        scope.launch { drawerState.close() }
                    },
                    modelSelectionContent = {
                        // 将重构后的模型选择下拉菜单放置在这里
                        ModelDropdowns()
                    }
                )
            },
            bottomBar = {
                ChatInputBar(
                    input = inputText,
                    onInputChange = { viewModel.onInputChange(it) },
                    onSendClick = { viewModel.sendMessageStream() }
                )
            }
        ) { innerPadding ->
            // 如果没有当前对话，可以显示一个欢迎界面或空状态
            currentConversation?.let { conversation ->
                LaunchedEffect(conversation.messages.size) {
                    Log.d("ChatScreenNew", "Rendering messages, count=${conversation.messages.size}")
                    conversation.messages.forEachIndexed { index, message ->
                        Log.d("ChatScreenNew", "Message $index: role=${message.role}, content=${message.content.take(50)}")
                    }
                }
                MessageList(
                    messages = conversation.messages,
                    modifier = Modifier.padding(innerPadding)
                )
            } ?: run {
                // 当 currentConversation 为 null 时的备用UI
                LaunchedEffect(Unit) {
                    Log.d("ChatScreenNew", "currentConversation is null")
                }
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    // 可以放置一个加载指示器或欢迎信息
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenNewPreview() {
    PhoneAiAssistantTheme {
        // 由于 ChatScreen 依赖于 ViewModel，
        // 在不提供真实 ViewModel 的情况下进行预览可能会比较复杂。
        // 一个简单的预览可以只渲染UI框架。
        // 这里我们假设有一个空的 ViewModel。
        ChatScreenNew()
    }
}