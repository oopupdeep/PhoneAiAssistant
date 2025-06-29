package com.wang.phoneaiassistant.ui.chat.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wang.phoneaiassistant.ui.theme.PhoneAiAssistantTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopAppBar(
    onNavigationIconClick: () -> Unit,
    onNewChatClick: () -> Unit,
    modelSelectionContent: @Composable () -> Unit
) {
    TopAppBar(
        title = { modelSelectionContent() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        // 需求 1: 左侧抽屉菜单图标
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "打开导航栏"
                )
            }
        },
        // 需求 2: 右侧新建对话图标
        actions = {
            IconButton(onClick = onNewChatClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新对话"
                )
            }
        }
    )
}

@Preview
@Composable
fun ChatTopAppBarPreview() {
    PhoneAiAssistantTheme {
        ChatTopAppBar(
            onNavigationIconClick = {},
            onNewChatClick = {},
            modelSelectionContent = { Text("模型选择区域") }
        )
    }
}