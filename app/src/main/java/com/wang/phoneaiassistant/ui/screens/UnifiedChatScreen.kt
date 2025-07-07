package com.wang.phoneaiassistant.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wang.phoneaiassistant.R
import com.wang.phoneaiassistant.data.ChatMode
import com.wang.phoneaiassistant.ui.chat.components.MessageList
import com.wang.phoneaiassistant.ui.chat.components.ChatInputBar
import com.wang.phoneaiassistant.ui.chat.components.ModelDropdowns
import com.wang.phoneaiassistant.ui.components.ChatTopAppBar
import com.wang.phoneaiassistant.ui.chat.ChatViewModel
import com.wang.phoneaiassistant.ui.screens.WebViewViewModel
import com.wang.phoneaiassistant.ui.viewmodels.UnifiedChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedChatScreen(
    onNavigateToSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    webViewViewModel: WebViewViewModel = hiltViewModel(),
    unifiedViewModel: UnifiedChatViewModel = hiltViewModel()
) {
    val currentMode by unifiedViewModel.currentMode.collectAsStateWithLifecycle()
    val showPasteKeyDialog by unifiedViewModel.showPasteKeyDialog.collectAsStateWithLifecycle()
    val currentBackgroundUri by unifiedViewModel.currentBackgroundUri.collectAsStateWithLifecycle()
    
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val inputText by chatViewModel.inputText
    
    // 调试日志
    LaunchedEffect(messages) {
        android.util.Log.d("UnifiedChatScreen", "Messages collected: ${messages.size}")
    }
    
    LaunchedEffect(currentBackgroundUri) {
        android.util.Log.d("UnifiedChatScreen", "Background URI: $currentBackgroundUri")
    }
    
    val webViewProgress by webViewViewModel.loadingProgress.collectAsStateWithLifecycle()
    val webViewError by webViewViewModel.error.collectAsStateWithLifecycle()
    
    val coroutineScope = rememberCoroutineScope()
    
    // 添加切换动画状态
    var isTransitioning by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentMode) {
        isTransitioning = true
        kotlinx.coroutines.delay(300)
        isTransitioning = false
    }
    
    // 粘贴API Key对话框
    if (showPasteKeyDialog) {
        PasteApiKeyDialog(
            onDismiss = { 
                unifiedViewModel.dismissPasteKeyDialog()
            },
            onKeyPasted = { apiKey ->
                // 保存API Key并切换到API模式
                chatViewModel.saveApiKey(apiKey)
                unifiedViewModel.switchToApiMode()
                unifiedViewModel.dismissPasteKeyDialog()
            },
            onSkip = {
                // 直接体验，保持WebView模式
                unifiedViewModel.dismissPasteKeyDialog()
            }
        )
    }
    
    Scaffold(
        topBar = {
            ChatTopAppBar(
                currentCompany = if (currentMode == ChatMode.WEBVIEW) {
                    unifiedViewModel.getWebViewProvider()
                } else {
                    unifiedViewModel.getCurrentCompany()
                },
                currentModel = unifiedViewModel.getCurrentModel(),
                currentMode = currentMode,
                onNavigationClick = onOpenDrawer,
                onNewChatClick = {
                    if (currentMode == ChatMode.API) {
                        chatViewModel.clearMessages()
                    } else {
                        webViewViewModel.reload()
                    }
                },
                onModeSwitch = { newMode ->
                    if (newMode == ChatMode.API && !unifiedViewModel.hasValidApiKey()) {
                        // 提示需要先填写API Key
                        unifiedViewModel.showPasteKeyDialog()
                    } else {
                        unifiedViewModel.setMode(newMode)
                    }
                },
                modelSelectionContent = {
                    // 在API模式下显示模型选择下拉菜单
                    ModelDropdowns(
                        viewModel = chatViewModel
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 消息列表区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Crossfade(
                    targetState = currentMode,
                    animationSpec = tween(300)
                ) { mode ->
                    when (mode) {
                        ChatMode.API -> {
                            // API模式：本地渲染消息
                            MessageList(
                                messages = messages,
                                backgroundUri = currentBackgroundUri,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        ChatMode.WEBVIEW -> {
                            // WebView模式：嵌入网页
                            WebViewContainer(
                                url = getWebViewUrl(unifiedViewModel.getWebViewProvider()),
                                progress = webViewProgress,
                                error = webViewError,
                                onProgressChange = { webViewViewModel.updateProgress(it) },
                                onError = { webViewViewModel.setError(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                
                // 切换时的加载指示器
                if (isTransitioning) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            // 输入框（两种模式共用）
            ChatInputBar(
                input = inputText,
                onInputChange = { chatViewModel.updateInputText(it) },
                onSendClick = {
                    if (currentMode == ChatMode.API) {
                        chatViewModel.sendMessageStream()
                    }
                    // WebView模式下输入框仅作装饰，实际输入在网页内
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PasteApiKeyDialog(
    onDismiss: () -> Unit,
    onKeyPasted: (String) -> Unit,
    onSkip: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置 API Key") },
        text = {
            Column {
                Text("粘贴您的 API Key 以使用 API 模式，或直接体验 WebView 模式。")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (apiKey.isNotBlank()) {
                        onKeyPasted(apiKey)
                    }
                },
                enabled = apiKey.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("直接体验")
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    url: String,
    progress: Int,
    error: String?,
    onProgressChange: (Int) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            onProgressChange(newProgress)
                        }
                    }
                    
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            onError(error?.description?.toString() ?: "加载失败")
                        }
                    }
                    
                    loadUrl(url)
                    webView = this
                }
            },
            update = { _ ->
                // 不在update中重新加载URL，避免不必要的刷新
            }
        )
        
        // 加载进度条
        if (progress < 100) {
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
        
        // 错误提示
        error?.let {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun getWebViewUrl(company: String?): String {
    return when (company?.lowercase()) {
        "deepseek" -> "https://chat.deepseek.com"
        "qwen", "通义千问" -> "https://tongyi.aliyun.com/qianwen"
        else -> "https://chat.deepseek.com"
    }
}