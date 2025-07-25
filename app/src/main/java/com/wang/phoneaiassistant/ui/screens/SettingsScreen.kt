package com.wang.phoneaiassistant.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.wang.phoneaiassistant.data.ChatMode
import com.wang.phoneaiassistant.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val companies = viewModel.getCompanyNames()
    
    var selectedCompany by remember { mutableStateOf(viewModel.getCurrentCompany()) }
    var apiKey by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 使用模式选择
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "使用模式",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentMode == ChatMode.API,
                            onClick = {
                                if (viewModel.hasValidApiKey()) {
                                    viewModel.setMode(ChatMode.API)
                                } else {
                                    showApiKeyDialog = true
                                }
                            },
                            label = { Text("API 模式") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        FilterChip(
                            selected = currentMode == ChatMode.WEBVIEW,
                            onClick = { viewModel.setMode(ChatMode.WEBVIEW) },
                            label = { Text("WebView 模式") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Text(
                        text = when (currentMode) {
                            ChatMode.API -> "使用 API Key 直接调用 AI 服务"
                            ChatMode.WEBVIEW -> "通过网页版使用 AI 服务"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // API Key 管理
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "API Key 管理",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    companies.forEach { company ->
                        val savedApiKey = viewModel.getApiKey(company)
                        
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = company,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    
                                    if (!savedApiKey.isNullOrBlank()) {
                                        AssistChip(
                                            onClick = { },
                                            label = { Text("已配置") }
                                        )
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (savedApiKey.isNullOrBlank()) {
                                        Button(
                                            onClick = {
                                                selectedCompany = company
                                                showApiKeyDialog = true
                                            }
                                        ) {
                                            Text("添加 API Key")
                                        }
                                    } else {
                                        TextButton(
                                            onClick = {
                                                selectedCompany = company
                                                apiKey = savedApiKey
                                                showApiKeyDialog = true
                                            }
                                        ) {
                                            Text("修改")
                                        }
                                        
                                        TextButton(
                                            onClick = {
                                                viewModel.deleteApiKey(company)
                                            },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text("删除")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // WebView提供商选择
            if (currentMode == ChatMode.WEBVIEW) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AI 服务提供商",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        var selectedProvider by remember { mutableStateOf(viewModel.getWebViewProvider()) }
                        val providers = listOf("DeepSeek", "Qwen")
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            providers.forEach { provider ->
                                FilterChip(
                                    selected = selectedProvider == provider,
                                    onClick = {
                                        selectedProvider = provider
                                        viewModel.setWebViewProvider(provider)
                                    },
                                    label = { Text(provider) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            
            // 聊天背景设置（仅API模式显示）
            if (currentMode == ChatMode.API) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "聊天背景",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        val backgroundUri by viewModel.backgroundUri.collectAsStateWithLifecycle()
                        val context = LocalContext.current
                        
                        val imagePicker = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            uri?.let {
                                // 获取持久化的URI权限
                                context.contentResolver.takePersistableUriPermission(
                                    it,
                                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                                viewModel.updateBackgroundUri(it.toString())
                            }
                        }
                        
                        // 背景预览和选择按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 背景预览
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (backgroundUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = backgroundUri),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Wallpaper,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // 操作按钮
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { imagePicker.launch("image/*") }
                                ) {
                                    Text("选择背景图片")
                                }
                                
                                if (backgroundUri != null) {
                                    TextButton(
                                        onClick = { viewModel.updateBackgroundUri(null) },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("移除背景")
                                    }
                                }
                            }
                        }
                        
                        Text(
                            text = "从相册选择一张图片作为聊天背景",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // API Key 输入对话框
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { 
                showApiKeyDialog = false
                apiKey = ""
            },
            title = { Text("设置 $selectedCompany API Key") },
            text = {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (apiKey.isNotBlank()) {
                            viewModel.saveApiKey(selectedCompany, apiKey)
                            showApiKeyDialog = false
                            apiKey = ""
                        }
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showApiKeyDialog = false
                        apiKey = ""
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}