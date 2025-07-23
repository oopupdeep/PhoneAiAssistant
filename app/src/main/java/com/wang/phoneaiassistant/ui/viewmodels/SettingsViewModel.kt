package com.wang.phoneaiassistant.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wang.phoneaiassistant.data.ChatMode
import com.wang.phoneaiassistant.data.ChatModeManager
import com.wang.phoneaiassistant.data.Authenticate.CompanyManager
import com.wang.phoneaiassistant.data.preferences.AppPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val chatModeManager: ChatModeManager,
    private val companyManager: CompanyManager,
    private val appPreferences: AppPreference
) : ViewModel() {
    
    val currentMode: StateFlow<ChatMode> = chatModeManager.currentMode
    
    private val _backgroundUri = MutableStateFlow(appPreferences.chatBackgroundUri)
    val backgroundUri: StateFlow<String?> = _backgroundUri.asStateFlow()
    
    fun getCompanyNames(): List<String> {
        return companyManager.getCompanyNames()
    }
    
    fun getCurrentCompany(): String {
        return chatModeManager.getCurrentCompany()
    }
    
    fun getCurrentModel(): String {
        return chatModeManager.getCurrentModel()
    }
    
    fun setMode(mode: ChatMode) {
        chatModeManager.setMode(mode)
    }
    
    fun hasValidApiKey(): Boolean {
        return chatModeManager.hasValidApiKey()
    }
    
    fun getApiKey(company: String): String? {
        return companyManager.getApiKey(company)
    }
    
    fun saveApiKey(company: String, apiKey: String) {
        viewModelScope.launch {
            companyManager.saveApiKey(company, apiKey)
        }
    }
    
    fun deleteApiKey(company: String) {
        viewModelScope.launch {
            // 只删除API Key，不删除整个公司配置
            companyManager.saveApiKey(company, "")
            if (!chatModeManager.hasValidApiKey()) {
                chatModeManager.setMode(ChatMode.WEBVIEW)
            }
        }
    }
    
    fun getWebViewProvider(): String {
        return chatModeManager.getWebViewProvider()
    }
    
    fun setWebViewProvider(provider: String) {
        chatModeManager.setWebViewProvider(provider)
    }
    
    fun updateBackgroundUri(uri: String?) {
        viewModelScope.launch {
            appPreferences.chatBackgroundUri = uri
            _backgroundUri.value = uri
        }
    }
}