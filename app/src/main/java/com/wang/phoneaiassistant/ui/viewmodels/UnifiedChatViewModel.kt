package com.wang.phoneaiassistant.ui.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wang.phoneaiassistant.data.ChatMode
import com.wang.phoneaiassistant.data.ChatModeManager
import com.wang.phoneaiassistant.data.preferences.AppPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnifiedChatViewModel @Inject constructor(
    private val chatModeManager: ChatModeManager,
    private val appPreferences: AppPreference
) : ViewModel() {
    
    val currentMode: StateFlow<ChatMode> = chatModeManager.currentMode
    val showPasteKeyDialog: StateFlow<Boolean> = chatModeManager.showPasteKeyDialog
    
    private val _currentBackgroundUri = MutableStateFlow(appPreferences.chatBackgroundUri)
    val currentBackgroundUri: StateFlow<String?> = _currentBackgroundUri.asStateFlow()
    
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "chat_background_uri") {
            _currentBackgroundUri.value = appPreferences.chatBackgroundUri
        }
    }
    
    init {
        appPreferences.prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }
    
    override fun onCleared() {
        super.onCleared()
        appPreferences.prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
    
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
    
    fun updateChatBackgroundUri(uri: String?) {
        viewModelScope.launch {
            appPreferences.chatBackgroundUri = uri
            _currentBackgroundUri.value = uri
        }
    }
}