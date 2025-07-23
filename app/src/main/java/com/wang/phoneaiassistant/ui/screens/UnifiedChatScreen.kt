package com.wang.phoneaiassistant.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    val contextMemoryEnabled by chatViewModel.contextMemoryEnabled.collectAsStateWithLifecycle()
    val isLoadingState by chatViewModel.isLoadingState.collectAsStateWithLifecycle()
    
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
    
    // WebView 导航状态
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    
    // 添加切换动画状态
    var isTransitioning by remember { mutableStateOf(false) }
    
    // 记录上一个模式，用于检测模式切换
    var previousMode by remember { mutableStateOf(currentMode) }
    
    LaunchedEffect(currentMode) {
        if (previousMode != currentMode) {
            isTransitioning = true
            previousMode = currentMode
            kotlinx.coroutines.delay(300)
            isTransitioning = false
        }
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
                onNavigationClick = if (currentMode == ChatMode.API) onOpenDrawer else null,
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
                onWebViewBack = {
                    webViewRef?.goBack()
                },
                onWebViewForward = {
                    webViewRef?.goForward()
                },
                canGoBack = canGoBack,
                canGoForward = canGoForward,
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
                                isStreaming = isLoadingState,
                                onCancelStream = { chatViewModel.cancelStreamResponse() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        ChatMode.WEBVIEW -> {
                            // WebView模式：嵌入网页
                            WebViewContainer(
                                webViewViewModel = webViewViewModel,
                                url = getWebViewUrl(unifiedViewModel.getWebViewProvider()),
                                progress = webViewProgress,
                                error = webViewError,
                                onProgressChange = { webViewViewModel.updateProgress(it) },
                                onError = { webViewViewModel.setError(it) },
                                onWebViewCreated = { webView ->
                                    webViewRef = webView
                                },
                                onNavigationStateChanged = { back, forward ->
                                    canGoBack = back
                                    canGoForward = forward
                                },
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
            
            // 输入框（仅在 API 模式下显示）
            if (currentMode == ChatMode.API) {
                ChatInputBar(
                    input = inputText,
                    onInputChange = { chatViewModel.updateInputText(it) },
                    onSendClick = {
                        chatViewModel.sendMessageStream()
                    },
                    contextMemoryEnabled = contextMemoryEnabled,
                    onContextMemoryToggle = { chatViewModel.toggleContextMemory() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
    webViewViewModel: WebViewViewModel,
    url: String,
    progress: Int,
    error: String?,
    onProgressChange: (Int) -> Unit,
    onError: (String) -> Unit,
    onWebViewCreated: (WebView) -> Unit = {},
    onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    val uiState by webViewViewModel.uiState.collectAsStateWithLifecycle()
    
    // 监听 reload 信号
    LaunchedEffect(uiState.error) {
        if (uiState.error == null && error == null && webView != null) {
            webView?.reload()
        }
    }
    
    // 处理 WebView 生命周期
    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                clearHistory()
                clearCache(true)
                loadUrl("about:blank")
                pauseTimers()
                onPause()
                removeAllViews()
                destroy()
            }
        }
    }
    
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
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // 确保页面加载完成后进度条消失
                            onProgressChange(100)
                            // 更新导航状态
                            view?.let {
                                onNavigationStateChanged(it.canGoBack(), it.canGoForward())
                            }
                        }
                        
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            // 更新导航状态
                            view?.let {
                                onNavigationStateChanged(it.canGoBack(), it.canGoForward())
                            }
                        }
                    }
                    
                    loadUrl(url)
                    webView = this
                    onWebViewCreated(this)
                }
            },
            update = { view ->
                // 确保 WebView 仍然有效且 URL 已改变
                if (view.url != url && url.isNotEmpty()) {
                    view.loadUrl(url)
                }
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
                    .clickable { webViewViewModel.clearError() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextButton(
                            onClick = { webViewViewModel.reload() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("重试")
                        }
                        TextButton(
                            onClick = { webViewViewModel.clearError() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("关闭")
                        }
                    }
                }
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