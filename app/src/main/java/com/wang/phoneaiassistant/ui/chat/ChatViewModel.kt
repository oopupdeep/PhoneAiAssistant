package com.wang.phoneaiassistant.ui.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wang.phoneaiassistant.data.network.AiApiService
import com.wang.phoneaiassistant.data.network.entity.Message
import com.wang.phoneaiassistant.data.network.entity.ModelInfo
import com.wang.phoneaiassistant.data.network.entity.ChatRequest
import com.wang.phoneaiassistant.data.repository.ModelRepository
import kotlinx.coroutines.launch

class ChatViewModel(private val baseUrl: String, private val repository: ModelRepository) : ViewModel() {

    var messages = mutableStateListOf(
        Message("assistant", "你好，我是你的 AI 助手！")
    )
        private set

    var inputText = mutableStateOf("")
        private set

    var selectedModel = mutableStateOf(
        ModelInfo("gpt-4", "OpenAI GPT-4", "deepseek")
    )
        private set

    private val _models = MutableLiveData<List<ModelInfo>>()
    val models: LiveData<List<ModelInfo>> get() = _models

    fun loadModels() {
        viewModelScope.launch {
            val result = repository.getAvailableModels()
            _models.value = result
        }
    }

    fun onInputChange(newText: String) {
        inputText.value = newText
    }

    fun onModelSelected(model: ModelInfo) {
        selectedModel.value = model
    }

    fun sendMessage() {
        val text = inputText.value.trim()
        if (text.isBlank()) return

        // 添加用户消息
        messages.add(Message("user", text))
        val chatMessage = ChatRequest(selectedModel.value.id, messages)


        // 模拟助手回复
        val reply = "（${selectedModel.value.name} 回复）这是回复内容。"
        messages.add(Message("assistant", reply))

        // 清空输入框
        inputText.value = ""
    }
}