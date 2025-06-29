package com.wang.phoneaiassistant.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wang.phoneaiassistant.ui.chat.ChatViewModel

@Composable
fun AppDrawer(
    onConversationClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel()
) {
    // 从 ViewModel 中收集状态
    val conversations by viewModel.filteredConversations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

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
                        selected = false, // 可以根据当前对话ID来高亮
                        onClick = { onConversationClick(conversation.id) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }

            HorizontalDivider()

            // 底部设置按钮 (需求 5)
            NavigationDrawerItem(
                label = { Text("设置") },
                icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                selected = false,
                onClick = onSettingsClick,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}