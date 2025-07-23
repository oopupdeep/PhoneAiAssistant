package com.wang.phoneaiassistant.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WebViewViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()
    
    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun updateLoadingState(isLoading: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = isLoading)
        }
    }
    
    fun updateProgress(progress: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingProgress = progress)
            _loadingProgress.value = progress
        }
    }
    
    fun updateError(error: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = error)
            _error.value = error
            
            // 如果是连接错误，5秒后自动清除
            if (error != null && (error.contains("ERR_CONNECTION_REFUSED") || 
                error.contains("ERR_INTERNET_DISCONNECTED") ||
                error.contains("ERR_NAME_NOT_RESOLVED"))) {
                delay(5000)
                if (_error.value == error) {
                    clearError()
                }
            }
        }
    }
    
    fun setError(error: String?) {
        updateError(error)
    }
    
    fun reload() {
        // 清除错误并触发重新加载
        viewModelScope.launch {
            _error.value = null
            _uiState.value = _uiState.value.copy(error = null)
            _loadingProgress.value = 0
        }
    }
    
    fun clearError() {
        viewModelScope.launch {
            _error.value = null
            _uiState.value = _uiState.value.copy(error = null)
        }
    }
}

data class WebViewUiState(
    val isLoading: Boolean = true,
    val loadingProgress: Int = 0,
    val error: String? = null
)

enum class AiChatService(val title: String, val url: String) {
    DEEPSEEK("DeepSeek", "https://chat.deepseek.com"),
    QWEN("通义千问", "https://tongyi.aliyun.com/qianwen/")
}