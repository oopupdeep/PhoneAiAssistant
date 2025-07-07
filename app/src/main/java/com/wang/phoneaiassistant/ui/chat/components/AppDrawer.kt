package com.wang.phoneaiassistant.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wang.phoneaiassistant.ui.chat.ChatViewModel

@Composable
fun AppDrawer(
    onNavigateToSettings: () -> Unit,
    onCloseDrawer: () -> Unit,
    onConversationClick: ((String) -> Unit)? = null,
    onAiChatClick: ((com.wang.phoneaiassistant.ui.screens.AiChatService) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    // 从 ViewModel 中收集状态
    val conversations by viewModel.filteredConversations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()

    ModalDrawerSheet(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部搜索框 (需求 4)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("搜索对话历史") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                singleLine = true
            )

            HorizontalDivider()

            // 中间对话列表 (需求 3)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = conversation.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = conversation.id == currentConversationId,
                        onClick = { 
                            onConversationClick?.invoke(conversation.id)
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }

            HorizontalDivider()

            // AI 聊天服务按钮
            if (onAiChatClick != null) {
                NavigationDrawerItem(
                    label = { Text("DeepSeek Chat") },
                    icon = { Icon(Icons.Default.Language, contentDescription = "DeepSeek") },
                    selected = false,
                    onClick = { onAiChatClick(com.wang.phoneaiassistant.ui.screens.AiChatService.DEEPSEEK) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                NavigationDrawerItem(
                    label = { Text("通义千问") },
                    icon = { Icon(Icons.Default.Language, contentDescription = "通义千问") },
                    selected = false,
                    onClick = { onAiChatClick(com.wang.phoneaiassistant.ui.screens.AiChatService.QWEN) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                HorizontalDivider()
            }

            // 底部设置按钮 (需求 5)
            NavigationDrawerItem(
                label = { Text("设置") },
                icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                selected = false,
                onClick = onNavigateToSettings,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}