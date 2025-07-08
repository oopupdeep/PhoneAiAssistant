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
import com.wang.phoneaiassistant.data.repository.ConversationRepository
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
import kotlinx.coroutines.flow.map

@HiltViewModel
class ChatViewModel@Inject constructor(
    private val modelRepository: ModelRepository,
    private val chatRepository: ChatRepository,
    private val companyManager: CompanyManager,
    private val conversationRepository: ConversationRepository
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
    
    // 添加StateFlow版本供Compose使用
    private val _isLoadingState = MutableStateFlow(false)
    val isLoadingState: StateFlow<Boolean> = _isLoadingState.asStateFlow()

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // 1. 使用 StateFlow 管理所有对话的列表
    val conversations: StateFlow<List<Conversation>> = conversationRepository.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 2. 当前正在查看的对话ID
    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()
    
    // 当前完整的对话（包含消息）
    private val _currentConversationWithMessages = MutableStateFlow<Conversation?>(null)
    val currentConversationWithMessages: StateFlow<Conversation?> = _currentConversationWithMessages.asStateFlow()

    val currentConversation: StateFlow<Conversation?> = currentConversationWithMessages
    
    // 独立的消息列表 StateFlow，确保 UI 能正确更新
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

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
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    // Gson实例用于解析JSON
    private val gson = Gson()

    init {
        // 初始化时加载或创建第一个对话
        viewModelScope.launch {
            Log.d("ChatViewModel", "Init block: Starting to collect conversations")
            conversations.collect { allConversations ->
                Log.d("ChatViewModel", "Init block: Got ${allConversations.size} conversations")
                if (_currentConversationId.value == null) {
                    if (allConversations.isEmpty()) {
                        Log.d("ChatViewModel", "Init block: No conversations, creating new one")
                        // 如果没有对话，创建一个新的
                        val newConversation = conversationRepository.createConversation()
                        _currentConversationId.value = newConversation.id
                        loadConversationMessages(newConversation.id)
                    } else {
                        Log.d("ChatViewModel", "Init block: Loading latest conversation")
                        // 加载最新的对话
                        val latestConversation = allConversations.first()
                        _currentConversationId.value = latestConversation.id
                        loadConversationMessages(latestConversation.id)
                    }
                }
            }
        }
    }
    
    companion object {
        const val LOADING_MESSAGE_CONTENT = "正在努力获取信息..."
    }

    private suspend fun loadConversationMessages(conversationId: String) {
        Log.d("ChatViewModel", "Loading conversation messages for ID: $conversationId")
        val conversation = conversationRepository.getConversationWithMessages(conversationId)
        Log.d("ChatViewModel", "Loaded conversation: ${conversation?.id}, messages: ${conversation?.messages?.size}")
        _currentConversationWithMessages.value = conversation
        _messages.value = conversation?.messages?.toList() ?: emptyList()
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
            sendMessageSuspend()
        }
    }
    
    private suspend fun sendMessageSuspend() {
            val text = inputText.value.trim()
            if (text.isBlank()) return
            
            val conversationId = _currentConversationId.value ?: return
            val currentConvo = _currentConversationWithMessages.value ?: return

            // 保存用户消息到数据库
            val userMessage = Message("user", text)
            conversationRepository.saveMessage(conversationId, userMessage)
            
            // 更新内存中的对话
            currentConvo.messages.add(userMessage)
            _currentConversationWithMessages.value = currentConvo.copy(messages = currentConvo.messages.toMutableList())
            
            // 使用当前对话中的消息进行请求
            val chatMessage = ChatRequest(selectedModel.value.id, currentConvo.messages, false)
            val chatResponse = chatRepository.getChat(chatMessage)
            
            // 添加AI响应
            val assistantMessage = Message("assistant", chatResponse.choices?.get(0)?.message?.content ?: "抱歉，获取响应失败")
            conversationRepository.saveMessage(conversationId, assistantMessage)
            
            currentConvo.messages.add(assistantMessage)
            _currentConversationWithMessages.value = currentConvo.copy(messages = currentConvo.messages.toMutableList())

            // 清空输入框
            inputText.value = ""
            
            // 如果是第一条消息，生成对话标题
            if (currentConvo.messages.size <= 3) { // system + user + assistant
                generateConversationTitle(conversationId, text)
            }
    }

    fun sendMessageStream() {
        val userMessageContent = inputText.value.trim()
        if (userMessageContent.isBlank()) {
            Log.d("ChatViewModel", "sendMessageStream: message is blank")
            return
        }
        
        val conversationId = _currentConversationId.value 
        if (conversationId == null) {
            Log.e("ChatViewModel", "sendMessageStream: conversationId is null")
            return
        }
        
        val currentConvo = _currentConversationWithMessages.value
        if (currentConvo == null) {
            Log.e("ChatViewModel", "sendMessageStream: currentConvo is null")
            return
        }
        
        Log.d("ChatViewModel", "sendMessageStream called with message: $userMessageContent")
        Log.d("ChatViewModel", "Current conversation ID: $conversationId")
        Log.d("ChatViewModel", "Current conversation messages count: ${currentConvo.messages.size}")
        
        // 清空输入框
        inputText.value = ""
        
        viewModelScope.launch {
            // 1. 保存用户消息到数据库
            val userMessage = Message("user", userMessageContent)
            conversationRepository.saveMessage(conversationId, userMessage)
            
            // 2. 更新内存中的对话 - 创建新的列表以触发StateFlow更新
            val updatedMessages = currentConvo.messages.toMutableList().apply {
                add(userMessage)
            }
            // 创建新的 Conversation 实例以确保 StateFlow 检测到变化
            val updatedConvo = Conversation(
                id = currentConvo.id,
                title = currentConvo.title,
                messages = updatedMessages
            )
            _currentConversationWithMessages.value = updatedConvo
            _messages.value = updatedMessages.toList() // 更新独立的消息列表
            Log.d("ChatViewModel", "Added user message, total messages: ${updatedMessages.size}")
            Log.d("ChatViewModel", "Updated _currentConversationWithMessages, id: ${updatedConvo.id}")
            
            // 3. 创建 AI 响应的占位消息
            _isLoadingState.value = true
            val assistantMessage = Message("assistant", LOADING_MESSAGE_CONTENT)
            val messagesWithLoading = updatedConvo.messages.toMutableList().apply {
                add(assistantMessage)
            }
            // 再次创建新的 Conversation 实例
            val convoWithLoading = Conversation(
                id = updatedConvo.id,
                title = updatedConvo.title,
                messages = messagesWithLoading
            )
            _currentConversationWithMessages.value = convoWithLoading
            _messages.value = messagesWithLoading.toList() // 更新独立的消息列表
            Log.d("ChatViewModel", "Added loading message, total messages: ${messagesWithLoading.size}")
            
            // 4. 创建聊天请求
            val chatRequest = ChatRequest(
                model = selectedModel.value.id,
                messages = messagesWithLoading.toList(),
                stream = true
            )
            
            var isFirstChunk = true
            val accumulatedContent = StringBuilder()
            
            try {
                chatRepository.getChatStream(chatRequest)
                    .catch { e ->
                        // 处理流错误
                        _isLoadingState.value = false
                        val currentMessages = _currentConversationWithMessages.value?.messages?.toMutableList() ?: return@catch
                        val lastIndex = currentMessages.lastIndex
                        currentMessages[lastIndex] = currentMessages[lastIndex].copy(content = "Error: ${e.message}")
                        // 创建新的 Conversation 实例以触发更新
                        val currentConvo = _currentConversationWithMessages.value ?: return@catch
                        _currentConversationWithMessages.value = Conversation(
                            id = currentConvo.id,
                            title = currentConvo.title,
                            messages = currentMessages
                        )
                        _messages.value = currentMessages.toList() // 更新独立的消息列表
                    }
                    .collect { chunk ->
                        if (chunk.startsWith("data: ")) {
                            val jsonString = chunk.substring(6).trim()
                            if (jsonString == "[DONE]") {
                                // 流结束，保存最终的助手消息到数据库
                                _isLoadingState.value = false
                                val finalMessage = Message("assistant", accumulatedContent.toString())
                                conversationRepository.saveMessage(conversationId, finalMessage)
                                
                                // 如果是第一条消息，生成对话标题
                                val currentMsgCount = _currentConversationWithMessages.value?.messages?.size ?: 0
                                if (currentMsgCount <= 3) { // system + user + assistant
                                    generateConversationTitle(conversationId, userMessageContent)
                                }
                                return@collect
                            }
                            
                            try {
                                val streamResponse = gson.fromJson(jsonString, StreamResponse::class.java)
                                val deltaContent = streamResponse.choices.firstOrNull()?.delta?.content ?: ""
                                
                                if (deltaContent.isNotEmpty()) {
                                    accumulatedContent.append(deltaContent)
                                    
                                    val currentMessages = _currentConversationWithMessages.value?.messages?.toMutableList() ?: return@collect
                                    val lastIndex = currentMessages.lastIndex
                                    
                                    if (isFirstChunk) {
                                        currentMessages[lastIndex] = currentMessages[lastIndex].copy(content = deltaContent)
                                        isFirstChunk = false
                                    } else {
                                        currentMessages[lastIndex] = currentMessages[lastIndex].copy(
                                            content = currentMessages[lastIndex].content + deltaContent
                                        )
                                    }
                                    
                                    // 创建新的 Conversation 实例以触发更新
                                    val currentConvo = _currentConversationWithMessages.value ?: return@collect
                                    _currentConversationWithMessages.value = Conversation(
                                        id = currentConvo.id,
                                        title = currentConvo.title,
                                        messages = currentMessages
                                    )
                                    _messages.value = currentMessages.toList() // 更新独立的消息列表
                                    delay(50L)
                                }
                            } catch (e: Exception) {
                                Log.e("ChatViewModel", "Error parsing stream chunk", e)
                            }
                        }
                    }
            } catch (e: Exception) {
                // 处理请求错误
                _isLoadingState.value = false
                val currentMessages = _currentConversationWithMessages.value?.messages?.toMutableList() ?: return@launch
                val lastIndex = currentMessages.lastIndex
                currentMessages[lastIndex] = currentMessages[lastIndex].copy(content = "Request failed: ${e.message}")
                // 创建新的 Conversation 实例以触发更新
                val currentConvo = _currentConversationWithMessages.value ?: return@launch
                _currentConversationWithMessages.value = Conversation(
                    id = currentConvo.id,
                    title = currentConvo.title,
                    messages = currentMessages
                )
                _messages.value = currentMessages.toList() // 更新独立的消息列表
                
                // 保存错误消息到数据库
                conversationRepository.saveMessage(conversationId, currentMessages[lastIndex])
            }
        }
    }

    fun switchChat(conversationId: String) {
        Log.d("ChatViewModel", "switchChat called with conversationId: $conversationId")
        _currentConversationId.value = conversationId
        viewModelScope.launch {
            loadConversationMessages(conversationId)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun createNewChat() {
        viewModelScope.launch {
            val newConversation = conversationRepository.createConversation()
            _currentConversationId.value = newConversation.id
            loadConversationMessages(newConversation.id)
        }
    }
    
    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            val currentCompany = selectedCompany.value
            companyManager.saveApiKey(currentCompany, apiKey)
            companyManager.setCompany(currentCompany)
            loadModels()
        }
    }
    
    fun clearMessages() {
        val conversationId = _currentConversationId.value ?: return
        val currentConvo = _currentConversationWithMessages.value ?: return
        
        viewModelScope.launch {
            // 清空当前对话的消息
            currentConvo.messages.clear()
            _currentConversationWithMessages.value = currentConvo.copy(messages = mutableListOf())
            
            // 可以选择创建新对话或保持当前对话
            createNewChat()
        }
    }
    
    fun updateInputText(text: String) {
        inputText.value = text
    }
    
    private fun generateConversationTitle(conversationId: String, firstUserMessage: String) {
        viewModelScope.launch {
            try {
                // 使用AI生成对话标题
                val titlePrompt = """
                    请为以下对话生成一个简短的标题（不超过20个字）：
                    用户：$firstUserMessage
                    
                    要求：
                    1. 标题应该概括对话的主题
                    2. 使用简洁的语言
                    3. 只返回标题文本，不要有额外的说明
                """.trimIndent()
                
                val titleRequest = ChatRequest(
                    model = selectedModel.value.id,
                    messages = listOf(Message("user", titlePrompt)),
                    stream = false
                )
                
                val response = chatRepository.getChat(titleRequest)
                val generatedTitle = response.choices?.firstOrNull()?.message?.content?.trim() ?: firstUserMessage.take(30)
                
                // 确保标题不会太长
                val finalTitle = if (generatedTitle.length > 30) {
                    generatedTitle.substring(0, 30) + "..."
                } else {
                    generatedTitle
                }
                
                conversationRepository.updateConversationTitle(conversationId, finalTitle)
            } catch (e: Exception) {
                // 如果生成失败，使用简单的截断方式
                val fallbackTitle = if (firstUserMessage.length > 30) {
                    firstUserMessage.substring(0, 30) + "..."
                } else {
                    firstUserMessage
                }
                conversationRepository.updateConversationTitle(conversationId, fallbackTitle)
            }
        }
    }
    
    fun switchToConversation(conversationId: String) {
        viewModelScope.launch {
            _currentConversationId.value = conversationId
            loadConversationMessages(conversationId)
        }
    }
    
    fun createNewConversation() {
        viewModelScope.launch {
            val newConversation = conversationRepository.createConversation()
            _currentConversationId.value = newConversation.id
            loadConversationMessages(newConversation.id)
        }
    }
    
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            // 删除对话
            conversationRepository.deleteConversation(conversationId)
            
            // 如果删除的是当前对话，需要切换到其他对话或创建新对话
            if (_currentConversationId.value == conversationId) {
                val remainingConversations = conversations.value.filter { it.id != conversationId }
                if (remainingConversations.isNotEmpty()) {
                    // 切换到第一个剩余的对话
                    switchToConversation(remainingConversations.first().id)
                } else {
                    // 创建新对话
                    createNewConversation()
                }
            }
        }
    }
}