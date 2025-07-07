package com.wang.phoneaiassistant.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.wang.phoneaiassistant.data.ChatMode
import com.wang.phoneaiassistant.data.ChatModeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class UnifiedChatViewModel @Inject constructor(
    private val chatModeManager: ChatModeManager
) : ViewModel() {
    
    val currentMode: StateFlow<ChatMode> = chatModeManager.currentMode
    val showPasteKeyDialog: StateFlow<Boolean> = chatModeManager.showPasteKeyDialog
    
    fun dismissPasteKeyDialog() {
        chatModeManager.dismissPasteKeyDialog()
    }
    
    fun switchToApiMode(): Boolean {
        return chatModeManager.switchToApiMode()
    }
    
    fun setMode(mode: ChatMode) {
        chatModeManager.setMode(mode)
    }
    
    fun hasValidApiKey(): Boolean {
        return chatModeManager.hasValidApiKey()
    }
    
    fun showPasteKeyDialog() {
        chatModeManager.showPasteKeyDialog()
    }
    
    fun getCurrentCompany(): String {
        return chatModeManager.getCurrentCompany()
    }
    
    fun getCurrentModel(): String {
        return chatModeManager.getCurrentModel()
    }
    
    fun getWebViewProvider(): String {
        return chatModeManager.getWebViewProvider()
    }
}