package com.wang.phoneaiassistant.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapHoriz
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
    onNavigationClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onModeSwitch: (ChatMode) -> Unit,
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
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "打开菜单"
                )
            }
        },
        actions = {
            // 模式切换按钮
            IconButton(onClick = {
                val newMode = when (currentMode) {
                    ChatMode.API -> ChatMode.WEBVIEW
                    ChatMode.WEBVIEW -> ChatMode.API
                }
                onModeSwitch(newMode)
            }) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "切换模式"
                )
            }
            
            // 新对话按钮
            IconButton(onClick = onNewChatClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新对话"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}