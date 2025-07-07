package com.wang.phoneaiassistant.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wang.phoneaiassistant.ui.chat.ChatViewModel
import com.wang.phoneaiassistant.ui.chat.components.AppDrawer
import com.wang.phoneaiassistant.ui.screens.SettingsScreen
import com.wang.phoneaiassistant.ui.screens.UnifiedChatScreen
import kotlinx.coroutines.launch

sealed class UnifiedScreen(val route: String) {
    object Chat : UnifiedScreen("chat")
    object Settings : UnifiedScreen("settings")
}

@Composable
fun UnifiedAppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val chatViewModel: ChatViewModel = hiltViewModel()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                onNavigateToSettings = {
                    coroutineScope.launch {
                        drawerState.close()
                        navController.navigate(UnifiedScreen.Settings.route)
                    }
                },
                onCloseDrawer = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                },
                onConversationClick = { conversationId ->
                    // 切换到选中的对话
                    chatViewModel.switchChat(conversationId)
                    coroutineScope.launch {
                        drawerState.close()
                    }
                },
                viewModel = chatViewModel
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = UnifiedScreen.Chat.route
        ) {
            composable(UnifiedScreen.Chat.route) {
                UnifiedChatScreen(
                    onNavigateToSettings = {
                        navController.navigate(UnifiedScreen.Settings.route)
                    },
                    onOpenDrawer = {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                    },
                    chatViewModel = chatViewModel
                )
            }
            
            composable(UnifiedScreen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}