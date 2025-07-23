package com.wang.phoneaiassistant.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wang.phoneaiassistant.data.ChatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopAppBar(
    currentCompany: String,
    currentModel: String,
    currentMode: ChatMode,
    onNavigationClick: (() -> Unit)?,
    onNewChatClick: () -> Unit,
    onModeSwitch: (ChatMode) -> Unit,
    onWebViewBack: (() -> Unit)? = null,
    onWebViewForward: (() -> Unit)? = null,
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    modelSelectionContent: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            when (currentMode) {
                ChatMode.API -> {
                    // API模式下显示模型选择下拉菜单
                    modelSelectionContent()
                }
                ChatMode.WEBVIEW -> {
                    // WebView模式下只显示文本
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$currentCompany (网页版)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "WebView 模式",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        navigationIcon = {
            if (onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "打开菜单"
                    )
                }
            }
        },
        actions = {
            when (currentMode) {
                ChatMode.WEBVIEW -> {
                    // WebView 模式下显示后退、前进按钮
                    IconButton(
                        onClick = { onWebViewBack?.invoke() },
                        enabled = canGoBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "后退"
                        )
                    }
                    
                    IconButton(
                        onClick = { onWebViewForward?.invoke() },
                        enabled = canGoForward
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "前进"
                        )
                    }
                    
                    // 切换到 API 模式的按钮
                    TextButton(
                        onClick = { onModeSwitch(ChatMode.API) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "API",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                ChatMode.API -> {
                    // API 模式下显示新对话按钮
                    IconButton(onClick = onNewChatClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新对话"
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}