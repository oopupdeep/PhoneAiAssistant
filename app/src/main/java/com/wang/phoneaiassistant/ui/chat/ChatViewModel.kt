package com.wang.phoneaiassistant.ui.chat

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wang.phoneaiassistant.data.network.ChatService
import com.wang.phoneaiassistant.data.network.entity.Message
import com.wang.phoneaiassistant.data.network.entity.ModelInfo
import com.wang.phoneaiassistant.data.network.entity.ChatRequest
import com.wang.phoneaiassistant.data.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel@Inject constructor(
    private val modelRepository: ModelRepository,
    private val chatService: ChatService
) : ViewModel() {

    var messages = mutableStateListOf(
        Message("assistant", "你好，我是你的 AI 助手！")
    )
        private set

    var inputText = mutableStateOf("")
        private set

    var selectedModel = mutableStateOf(
        ModelInfo("gpt-4", "OpenAI GPT-4", "OpenAI")
    )
        private set

    private val _models = MutableLiveData<List<ModelInfo>>(emptyList())
    val models: LiveData<List<ModelInfo>> = _models

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

//    private val _models = MutableLiveData<List<ModelInfo>>()
//    val models: LiveData<List<ModelInfo>> get() = _models

//    val service = RetrofitProvider.getService(this, ModelService::class.java)
//    val modelRepository = ModelRepository(service)

    fun loadModels() {
        viewModelScope.launch {
            // 2. 更新 LiveData 的值
            // 在主线程协程中，可以直接使用 .value
            // 如果在非主线程，应使用 .postValue()
            _isLoading.value = true
            _error.value = null
            try {
                val modelList = modelRepository.getAvailableModels()
                _models.value = modelList
            } catch (e: Exception) {
                _error.value = "Failed to load models: ${e.message}"
                _models.value = emptyList()
            } finally {
                _isLoading.value = false
            }
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
        val reply = "（${selectedModel.value.id} 回复）这是回复内容。"
        messages.add(Message("assistant", reply))

        // 清空输入框
        inputText.value = ""
    }
}