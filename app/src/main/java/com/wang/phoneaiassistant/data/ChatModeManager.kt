package com.wang.phoneaiassistant.data

import android.content.Context
import android.content.SharedPreferences
import com.wang.phoneaiassistant.data.Authenticate.CompanyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ChatMode {
    API,
    WEBVIEW
}

@Singleton
class ChatModeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val companyManager: CompanyManager
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chat_mode_prefs", Context.MODE_PRIVATE)
    
    private val _currentMode = MutableStateFlow(loadSavedMode())
    val currentMode: StateFlow<ChatMode> = _currentMode.asStateFlow()
    
    private val _showPasteKeyDialog = MutableStateFlow(false)
    val showPasteKeyDialog: StateFlow<Boolean> = _showPasteKeyDialog.asStateFlow()
    
    init {
        // 启动时检测API Key
        checkInitialMode()
    }
    
    private fun checkInitialMode() {
        val hasApiKey = hasValidApiKey()
        
        if (!hasApiKey) {
            // 无Key，默认WebView模式，并显示粘贴Key对话框
            setMode(ChatMode.WEBVIEW)
            _showPasteKeyDialog.value = true
        } else {
            // 有Key，使用保存的模式或默认API模式
            val savedMode = loadSavedMode()
            if (savedMode == ChatMode.API) {
                setMode(ChatMode.API)
            }
        }
    }
    
    fun hasValidApiKey(): Boolean {
        val companies = companyManager.getCompanyNames()
        return companies.any { company ->
            val apiKey = companyManager.getApiKey(company)
            !apiKey.isNullOrBlank()
        }
    }
    
    fun setMode(mode: ChatMode) {
        _currentMode.value = mode
        saveMode(mode)
    }
    
    fun switchToApiMode(): Boolean {
        return if (hasValidApiKey()) {
            setMode(ChatMode.API)
            true
        } else {
            false
        }
    }
    
    fun dismissPasteKeyDialog() {
        _showPasteKeyDialog.value = false
    }
    
    fun showPasteKeyDialog() {
        _showPasteKeyDialog.value = true
    }
    
    private fun saveMode(mode: ChatMode) {
        prefs.edit().putString("chat_mode", mode.name).apply()
    }
    
    private fun loadSavedMode(): ChatMode {
        val savedMode = prefs.getString("chat_mode", ChatMode.WEBVIEW.name)
        return try {
            ChatMode.valueOf(savedMode ?: ChatMode.WEBVIEW.name)
        } catch (e: IllegalArgumentException) {
            ChatMode.WEBVIEW
        }
    }
    
    fun getCurrentCompany(): String {
        // 获取所有公司，返回第一个有API Key的公司
        val companies = companyManager.getCompanyNames()
        return companies.firstOrNull { company ->
            !companyManager.getApiKey(company).isNullOrBlank()
        } ?: "DeepSeek"
    }
    
    fun getCurrentModel(): String {
        return "默认模型"
    }
    
    fun getWebViewProvider(): String {
        return prefs.getString("webview_provider", "DeepSeek") ?: "DeepSeek"
    }
    
    fun setWebViewProvider(provider: String) {
        prefs.edit().putString("webview_provider", provider).apply()
    }
}