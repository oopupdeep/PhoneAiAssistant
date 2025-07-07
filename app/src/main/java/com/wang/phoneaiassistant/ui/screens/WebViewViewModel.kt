package com.wang.phoneaiassistant.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
        }
    }
    
    fun setError(error: String?) {
        updateError(error)
    }
    
    fun reload() {
        // 重置状态以触发WebView重新加载
        viewModelScope.launch {
            _uiState.value = WebViewUiState()
            _loadingProgress.value = 0
            _error.value = null
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