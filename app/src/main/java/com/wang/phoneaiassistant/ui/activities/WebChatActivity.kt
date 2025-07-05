package com.wang.phoneaiassistant.ui.activities

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.wang.phoneaiassistant.ui.theme.PhoneAiAssistantTheme

class WebChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoneAiAssistantTheme {
                WebChatScreen { finish() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebChatScreen(onFinish: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val titles = listOf("DeepSeek", "Qwen")
    val urls = listOf(
        "https://www.deepseek.com/",
        "https://qwen.tech/"
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web Chat") },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        loadUrl(urls[selectedTab])
                    }
                },
                update = { it.loadUrl(urls[selectedTab]) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
