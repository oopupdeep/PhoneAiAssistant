package com.wang.phoneaiassistant.ui.chat

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.wang.phoneaiassistant.data.Authenticate.Companies
import com.wang.phoneaiassistant.data.entity.network.ChatRequest
import com.wang.phoneaiassistant.data.entity.chat.Message
import com.wang.phoneaiassistant.data.entity.chat.ModelInfo
import com.wang.phoneaiassistant.data.entity.network.StreamResponse
import com.wang.phoneaiassistant.data.repository.ChatRepository
import com.wang.phoneaiassistant.data.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.wang.phoneaiassistant.data.Authenticate.CompanyManager
import com.wang.phoneaiassistant.data.entity.chat.Conversation
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ChatViewModel@Inject constructor(
    private val modelRepository: ModelRepository,
    private val chatRepository: ChatRepository,
    private val companyManager: CompanyManager
) : ViewModel() {

    // inputText, selectedModel, models等状态保持不变

    var inputText = mutableStateOf("")
        private set

    var selectedModel = mutableStateOf(
        ModelInfo("deepseek-chat", "DeepSeek", "DeepSeek")
    )
        private set

    var selectedCompany = mutableStateOf(
        Companies.DEEPSEEK
    )

    private val _models = MutableLiveData<List<ModelInfo>>(emptyList())
    val models: LiveData<List<ModelInfo>> = _models

    private val _companies = MutableLiveData<List<String>>(emptyList())
    val companies: LiveData<List<String>> = _companies

    private val _showApiInputDialogForCompany = MutableLiveData<Boolean?>(false)
    val showApiInputDialogForCompany: LiveData<Boolean?> = _showApiInputDialogForCompany

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // 1. 使用 StateFlow 管理所有对话的列表
    private val _conversations = MutableStateFlow<List<Conversation>>(
        listOf(Conversation(
            messages = mutableListOf(Message("system", "我是一名有用的AI助手"))
        ))
    )
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // 2. 当前正在查看的对话ID
    private val _currentConversationId = MutableStateFlow(_conversations.value.first().id)
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    val currentConversation: StateFlow<Conversation?> = combine(conversations, currentConversationId) { convos, id ->
        val result = convos.find { it.id == id }
        Log.d("ChatViewModel", "currentConversation: looking for id=$id, found=${result != null}, messages count=${result?.messages?.size}")
        result
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _conversations.value.firstOrNull())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredConversations: StateFlow<List<Conversation>> = combine(conversations, searchQuery) { convos, query ->
        if (query.isBlank()) {
            convos
        } else {
            convos.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.messages.any { msg -> msg.content.contains(query, ignoreCase = true) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, _conversations.value)


    // Gson实例用于解析JSON
    private val gson = Gson()

    companion object {
        const val LOADING_MESSAGE_CONTENT = "正在努力获取信息..."
    }

    fun loadCompanies() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val companyList = companyManager.getCompanyNames()
                _companies.value = companyList
            } catch (e: Exception) {
                _error.value = "Failed to load companies: ${e.message}"
                _companies.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

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
                if (modelList.isNotEmpty()) {
                    selectedModel.value = modelList.first()
                }
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

    fun onCompanySelected(company: String) {
        val preCompany = selectedCompany.value
        selectedCompany.value = company
        val apiKey = companyManager.getApiKey(company)
        if (apiKey.isNullOrEmpty()) {
            // 发出弹窗请求
            _showApiInputDialogForCompany.value = true
        } else {
            _showApiInputDialogForCompany.value = false
            // 设置公司
            companyManager.setCompany(company)
            loadModels()
        }
    }

    fun onApiKeySubmitted(company: String, apiKey: String) {
        // 在这里实现你的业务逻辑，例如：
        // 1. 验证 key 的格式 (可选)
        // 2. 将 company 和 apiKey 保存到 SharedPreferences 或数据库
         viewModelScope.launch {
             companyManager.saveApiKey(company, apiKey)
         }
        Log.i("ChatViewModel","API Key for $company submitted: $apiKey") // 示例打印

        // 3. 关闭对话框
        _showApiInputDialogForCompany.value = false
        companyManager.setCompany(company)
        loadModels()
    }

    /**
     * 当用户点击 "取消" 或对话框外部时，由UI层调用
     */
    fun onApiKeyDialogDismissed(prevCompany: String) {
        // 关闭对话框，将 LiveData 的值设为 null
        _showApiInputDialogForCompany.value = false
        selectedCompany.value = prevCompany
    }

    /**
     * 在你需要弹出对话框的地方调用这个方法
     * 例如，在 sendMessageStream 或 onCompanySelected 方法中检查到没有API Key时
     */
//    fun triggerApiKeyDialog(company: String) {
//        _showApiInputDialogForCompany.value = company
//    }


    fun sendMessage() {
        viewModelScope.launch {
            val text = inputText.value.trim()
            if (text.isBlank()) return@launch

            // 获取当前对话索引
            val currentIndex = _conversations.value.indexOfFirst { it.id == _currentConversationId.value }
            if (currentIndex == -1) return@launch
            
            val currentConvo = _conversations.value[currentIndex]

            // 添加用户消息到当前对话
            currentConvo.messages.add(Message("user", text))
            
            // 更新conversations以触发UI更新
            _conversations.value = _conversations.value.toMutableList().also {
                it[currentIndex] = currentConvo.copy(messages = currentConvo.messages.toMutableList())
            }
            
            // 使用当前对话中的消息进行请求
            val chatMessage = ChatRequest(selectedModel.value.id, currentConvo.messages, false)
            val chatResponse = chatRepository.getChat(chatMessage)
            
            // 获取最新的对话状态并添加响应
            val updatedConvo = _conversations.value[currentIndex]
            updatedConvo.messages.add(Message("system", chatResponse.choices?.get(0)?.message?.content ?: "null"))

            // 再次触发conversations的更新
            _conversations.value = _conversations.value.toMutableList().also {
                it[currentIndex] = updatedConvo.copy(messages = updatedConvo.messages.toMutableList())
            }

            // 清空输入框
            inputText.value = ""
        }
    }

    fun sendMessageStream() {
        val userMessageContent = inputText.value.trim()
        if (userMessageContent.isBlank()) return

        // 获取当前对话索引
        val currentIndex = _conversations.value.indexOfFirst { it.id == _currentConversationId.value }
        if (currentIndex == -1) return
        
        // 1. 创建新的消息列表并添加用户消息
        val updatedMessages = _conversations.value[currentIndex].messages.toMutableList()
        updatedMessages.add(Message("user", userMessageContent))
        Log.d("ChatViewModel", "Added user message, messages count=${updatedMessages.size}")
        
        // 创建新的对话对象和conversations列表以触发StateFlow更新
        val updatedConversation = _conversations.value[currentIndex].copy(messages = updatedMessages)
        _conversations.value = _conversations.value.toMutableList().apply {
            this[currentIndex] = updatedConversation
        }
        Log.d("ChatViewModel", "Updated conversations, current convo id=${updatedConversation.id}")

        // 准备一个包含历史消息的列表副本用于请求
        val messagesForRequest = updatedMessages.toList()

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
            val messagesWithLoading = _conversations.value[currentIndex].messages.toMutableList()
            messagesWithLoading.add(Message(role = "assistant", content = LOADING_MESSAGE_CONTENT))
            
            // 触发conversations的更新
            val conversationWithLoading = _conversations.value[currentIndex].copy(messages = messagesWithLoading)
            _conversations.value = _conversations.value.toMutableList().apply {
                this[currentIndex] = conversationWithLoading
            }
            
            // 新增一个标志位，用于判断是否是第一个数据块
            var isFirstChunk = true

            try {
                // 4. 调用仓库的流式方法并收集数据
                chatRepository.getChatStream(chatRequest)
                    .catch { e ->
                        // 处理流本身的异常
                        val errorMessages = _conversations.value[currentIndex].messages.toMutableList()
                        val lastMessageIndex = errorMessages.lastIndex
                        errorMessages[lastMessageIndex] = errorMessages[lastMessageIndex].copy(content = "Error: ${e.message}")
                        
                        val errorConversation = _conversations.value[currentIndex].copy(messages = errorMessages)
                        _conversations.value = _conversations.value.toMutableList().apply {
                            this[currentIndex] = errorConversation
                        }
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
                                    // 获取最新的消息列表
                                    val updatedMessages = _conversations.value[currentIndex].messages.toMutableList()
                                    val lastMessageIndex = updatedMessages.lastIndex
                                    val currentMessage = updatedMessages[lastMessageIndex]
                                    
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
                                    updatedMessages[lastMessageIndex] = updatedMessage
                                    
                                    // 触发conversations的更新
                                    val updatedConversation = _conversations.value[currentIndex].copy(messages = updatedMessages)
                                    _conversations.value = _conversations.value.toMutableList().apply {
                                        this[currentIndex] = updatedConversation
                                    }
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
                val failedMessages = _conversations.value[currentIndex].messages.toMutableList()
                val lastMessageIndex = failedMessages.lastIndex
                failedMessages[lastMessageIndex] = failedMessages[lastMessageIndex].copy(content = "Request failed: ${e.message}")
                
                val failedConversation = _conversations.value[currentIndex].copy(messages = failedMessages)
                _conversations.value = _conversations.value.toMutableList().apply {
                    this[currentIndex] = failedConversation
                }
            }
        }
    }

    fun switchChat(conversationId: String) {
        _currentConversationId.value = conversationId
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun createNewChat() {
        val newConversation = Conversation(
            messages = mutableListOf(Message("system", "我是一名有用的AI助手"))
        )
        _conversations.value = listOf(newConversation) + _conversations.value // 添加到列表顶部
        _currentConversationId.value = newConversation.id // 切换到新对话
    }
}