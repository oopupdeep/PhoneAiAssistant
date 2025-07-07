package com.wang.phoneaiassistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wang.phoneaiassistant.ui.chat.ChatScreenNew
import com.wang.phoneaiassistant.ui.screens.AiChatService
import com.wang.phoneaiassistant.ui.screens.AiServiceSelectionScreen
import com.wang.phoneaiassistant.ui.screens.WebViewScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object AiSelection : Screen("ai_selection")
    object Chat : Screen("chat")
    object WebView : Screen("webview/{url}/{title}/{useDesktopMode}") {
        fun createRoute(url: String, title: String, useDesktopMode: Boolean = false): String {
            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            return "webview/$encodedUrl/$encodedTitle/$useDesktopMode"
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.AiSelection.route
    ) {
        composable(Screen.AiSelection.route) {
            AiServiceSelectionScreen(
                onServiceSelected = { service ->
                    val useDesktopMode = service == AiChatService.QWEN
                    navController.navigate(
                        Screen.WebView.createRoute(service.url, service.title, useDesktopMode)
                    )
                },
                onUseApiMode = {
                    navController.navigate(Screen.Chat.route)
                }
            )
        }
        
        composable(Screen.Chat.route) {
            ChatScreenNew(
                onNavigateToAiChat = { service ->
                    val useDesktopMode = service == AiChatService.QWEN
                    navController.navigate(
                        Screen.WebView.createRoute(service.url, service.title, useDesktopMode)
                    )
                }
            )
        }
        
        composable(Screen.WebView.route) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
            val useDesktopMode = backStackEntry.arguments?.getString("useDesktopMode")?.toBoolean() ?: false
            val url = java.net.URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
            val title = java.net.URLDecoder.decode(encodedTitle, StandardCharsets.UTF_8.toString())
            
            WebViewScreen(
                url = url,
                title = title,
                useDesktopMode = useDesktopMode,
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
}