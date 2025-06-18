package com.wang.phoneaiassistant.ui.chat

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.wang.phoneaiassistant.data.network.ChatService
import com.wang.phoneaiassistant.data.network.entity.ChatRequest
import com.wang.phoneaiassistant.data.network.entity.Message
import com.wang.phoneaiassistant.data.network.entity.ModelInfo
import com.wang.phoneaiassistant.data.network.entity.StreamResponse
import com.wang.phoneaiassistant.data.repository.ChatRepository
import com.wang.phoneaiassistant.data.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel@Inject constructor(
    private val modelRepository: ModelRepository,
    private val chatRepository: ChatRepository,
    private val chatService: ChatService
) : ViewModel() {

    // messages, inputText, selectedModel, models等状态保持不变
    var messages = mutableStateListOf(
        Message("system", "我是一名有用的AI助手")
    )
        private set

    var inputText = mutableStateOf("")
        private set

    var selectedModel = mutableStateOf(
        ModelInfo("deepseek-chat", "DeepSeek", "DeepSeek")
    )
        private set

    private val _models = MutableLiveData<List<ModelInfo>>(emptyList())
    val models: LiveData<List<ModelInfo>> = _models

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // Gson实例用于解析JSON
    private val gson = Gson()

    companion object {
        const val LOADING_MESSAGE_CONTENT = "正在努力获取信息..."
    }

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
        viewModelScope.launch {
            val text = inputText.value.trim()
            if (text.isBlank()) return@launch

            // 添加用户消息
            messages.add(Message("user", text))
            val chatMessage = ChatRequest(selectedModel.value.id, messages, false)
            val chatResponse = chatRepository.getChat(chatMessage)
            messages.add(Message("system", chatResponse.choices?.get(0)?.message?.content ?: "null"))

            //        // 模拟助手回复
            //        val reply = "（${selectedModel.value.id} 回复）这是回复内容。"
            //        messages.add(Message("assistant", reply))

            // 清空输入框
            inputText.value = ""
        }
    }

    fun sendMessageStream() {
        val userMessageContent = inputText.value.trim()
        if (userMessageContent.isBlank()) return

        // 1. 添加用户消息到列表
        messages.add(Message("user", userMessageContent))

        // 准备一个包含历史消息的列表副本用于请求
        val messagesForRequest = messages.toList()

        // 2. 创建一个 stream = true 的请求
        val chatRequest = ChatRequest(
            model = selectedModel.value.id,
            messages = messagesForRequest,
            stream = true // <--- 关键：开启流式输出
        )

        // 清空输入框
        inputText.value = ""

        viewModelScope.launch {
            // 3. 立即为AI的回复添加一个空的占位消息
            messages.add(Message(role = "assistant", content = LOADING_MESSAGE_CONTENT))
            // 新增一个标志位，用于判断是否是第一个数据块
            var isFirstChunk = true

            try {
                // 4. 调用仓库的流式方法并收集数据
                chatRepository.getChatStream(chatRequest)
                    .catch { e ->
                        // 处理流本身的异常
                        val lastMessageIndex = messages.lastIndex
                        val currentMessage = messages[lastMessageIndex]
                        messages[lastMessageIndex] = currentMessage.copy(content = "Error: ${e.message}")
                    }
                    .collect { chunk ->
                        // chunk 的格式通常是 "data: {...}"
                        if (chunk.startsWith("data: ")) {
                            val jsonString = chunk.substring(6).trim()
                            if (jsonString == "[DONE]") {
                                // 流结束的标志
                                return@collect
                            }

                            try {
                                val streamResponse = gson.fromJson(jsonString, StreamResponse::class.java)
                                val deltaContent = streamResponse.choices.firstOrNull()?.delta?.content ?: ""

                                if (deltaContent.isNotEmpty()) {
                                    // 5. 更新列表中最后一条消息的内容
                                    val lastMessageIndex = messages.lastIndex
                                    val currentMessage = messages[lastMessageIndex]
                                    // 创建一个新消息对象来触发UI更新
                                    val updatedMessage: Message
                                    if (isFirstChunk) {
                                        // 如果是第一个数据块，则直接替换内容
                                        updatedMessage = currentMessage.copy(content = deltaContent)
                                        // 将标志位设为 false，这样后续的数据块就会走 else 分支
                                        isFirstChunk = false
                                    } else {
                                        // 如果不是第一个数据块，则在现有内容后追加
                                        updatedMessage = currentMessage.copy(content = currentMessage.content + deltaContent)
                                    }
                                    messages[lastMessageIndex] = updatedMessage
                                    delay(50L)
                                }
                            } catch (e: Exception) {
                                // 忽略无法解析的行
                                println("Could not parse stream chunk: $jsonString")
                            }
                        }
                    }
            } catch (e: Exception) {
                // 处理发起请求时的顶层异常
                val lastMessageIndex = messages.lastIndex
                val currentMessage = messages[lastMessageIndex]
                messages[lastMessageIndex] = currentMessage.copy(content = "Request failed: ${e.message}")
            }
        }
    }
}