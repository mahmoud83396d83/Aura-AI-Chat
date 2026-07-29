package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import com.example.data.ChatSession
import com.example.data.api.AiRetrofitClient
import com.example.data.api.OpenRouterModelItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ChatUiState {
    object Idle : ChatUiState
    object Loading : ChatUiState
    data class Success(val messages: List<ChatMessage>) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

class ChatViewModel(
    application: Application,
    private val repository: ChatRepository
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("smart_agent_prefs", Context.MODE_PRIVATE)

    // --- State variables ---
    private val _activeSessionId = MutableStateFlow<Int?>(null)
    val activeSessionId: StateFlow<Int?> = _activeSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSessionMessages: StateFlow<List<ChatMessage>> = _activeSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessages(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Key override (OpenRouter Only as requested)
    private val _openRouterApiKey = MutableStateFlow(sharedPrefs.getString("openrouter_key", "") ?: "")
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    // OpenRouter Models State
    private val _availableModels = MutableStateFlow<List<OpenRouterModelItem>>(emptyList())
    val availableModels: StateFlow<List<OpenRouterModelItem>> = _availableModels.asStateFlow()

    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

    // Model selection (default to a top free openrouter model)
    private val _selectedModel = MutableStateFlow(sharedPrefs.getString("selected_model", "deepseek/deepseek-r1:free") ?: "deepseek/deepseek-r1:free")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _customModel = MutableStateFlow(sharedPrefs.getString("custom_model", "") ?: "")
    val customModel: StateFlow<String> = _customModel.asStateFlow()

    // Agent Personality
    private val _selectedPersonality = MutableStateFlow(Personality.GENERAL)
    val selectedPersonality: StateFlow<Personality> = _selectedPersonality.asStateFlow()

    init {
        // Automatically create a default session if there are none on startup
        viewModelScope.launch {
            try {
                val sessionList = sessions.first()
                if (sessionList.isEmpty()) {
                    createNewSession("محادثة جديدة 1")
                } else if (_activeSessionId.value == null) {
                    _activeSessionId.value = sessionList.first().id
                }
            } catch (e: Throwable) {
                Log.e("ChatViewModel", "Error initializing session", e)
            }
        }

        // Fetch OpenRouter models dynamically
        fetchOpenRouterModels()
    }

    fun fetchOpenRouterModels() {
        viewModelScope.launch {
            _isFetchingModels.value = true
            try {
                val response = AiRetrofitClient.openRouterApi.getModels()
                response.data?.let { models ->
                    _availableModels.value = models
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to fetch OpenRouter models", e)
            } finally {
                _isFetchingModels.value = false
            }
        }
    }

    enum class Personality(val displayName: String, val description: String, val systemInstruction: String) {
        GENERAL(
            "General Assistant", 
            "Smart general purpose helper chatbot", 
            "You are a world-class AI Chat Assistant named Aura. You can write code, analyze data, generate tables/charts, create slides/presentations, and answer detailed questions. Answer clearly with clean Markdown formatting."
        ),
        DEVELOPER(
            "Coding Agent", 
            "Expert coding assistance & generation", 
            "You are Aura Developer Agent. You write perfect, secure, clean, and highly optimized code (Kotlin, Java, Python, JavaScript, SQL etc.). Explain choices concisely, use Markdown formatting with proper code blocks, and adhere to industry best practices."
        ),
        DATA_EXTRACTOR(
            "Data & Excel Expert", 
            "Structured tables, Excel CSV & metrics", 
            "You are a specialized Data & Excel Specialist Agent. Output structured tables, CSV data formats, metrics, and organized datasets that can be easily exported or viewed as spreadsheets."
        ),
        SLIDES_CREATOR(
            "Slides & Presentation Maker", 
            "PowerPoint, Pitch Decks & Document Slides", 
            "You are a Presentation & Document Designer Agent. Format responses into structured presentation slide sections (using '---' between slides, with clear Titles, Bullet points, Key Takeaways, and Visual summaries) ideal for exporting to PowerPoint, PDF, or document slides."
        ),
        CREATIVE_WRITER(
            "Creative Writer", 
            "Brilliant creative writer & translator", 
            "You are a master copywriter and creative thinker. Write brilliant prose, engaging marketing copies, poetry, or accurate linguistic translations with flair."
        )
    }

    fun selectSession(sessionId: Int) {
        _activeSessionId.value = sessionId
    }

    fun createNewSession(title: String) {
        viewModelScope.launch {
            val id = repository.createSession(title)
            _activeSessionId.value = id.toInt()
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = sessions.value.firstOrNull { it.id != sessionId }?.id
            }
        }
    }

    fun clearHistory() {
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            repository.clearSessionMessages(sessionId)
        }
    }

    fun selectModel(model: String) {
        _selectedModel.value = model
        sharedPrefs.edit().putString("selected_model", model).apply()
    }

    fun saveCustomModel(model: String) {
        _customModel.value = model
        sharedPrefs.edit().putString("custom_model", model).apply()
    }

    fun selectPersonality(personality: Personality) {
        _selectedPersonality.value = personality
    }

    fun saveOpenRouterApiKey(key: String) {
        _openRouterApiKey.value = key
        sharedPrefs.edit().putString("openrouter_key", key).apply()
        fetchOpenRouterModels()
    }

    fun sendMessage(content: String, imageUri: String? = null) {
        if (content.isBlank() && imageUri.isNullOrBlank()) return
        val sessionId = _activeSessionId.value ?: return

        viewModelScope.launch {
            // Save user message
            val userMsg = ChatMessage(
                sessionId = sessionId,
                role = "user",
                content = content,
                imageUri = imageUri
            )
            repository.saveMessage(userMsg)

            _isAiLoading.value = true

            // Resolve API key
            val finalOpenRouterKey = _openRouterApiKey.value.ifBlank { BuildConfig.OPENROUTER_API_KEY }

            val model = _selectedModel.value

            // Retrieve conversation history
            val history = activeSessionMessages.value

            // Send to AI
            val aiResponse = repository.sendAiRequest(
                sessionId = sessionId,
                modelName = model,
                isGemini = false,
                messages = history,
                systemInstruction = _selectedPersonality.value.systemInstruction,
                geminiApiKey = "",
                openRouterApiKey = finalOpenRouterKey
            )

            // Save AI response
            repository.saveMessage(aiResponse)

            // Update session title dynamically if it is a placeholder
            val currentSession = sessions.value.find { it.id == sessionId }
            if (currentSession != null && (currentSession.title.startsWith("First Chat") || currentSession.title == "New Chat")) {
                val previewTitle = if (content.length > 25) content.take(22) + "..." else if (!imageUri.isNullOrBlank()) "Photo Query" else content
                repository.updateSessionTitle(sessionId, previewTitle)
            }

            _isAiLoading.value = false
        }
    }

    fun generateImage(prompt: String) {
        if (prompt.isBlank()) return
        val sessionId = _activeSessionId.value ?: return

        viewModelScope.launch {
            // Save user prompt
            val userMsg = ChatMessage(
                sessionId = sessionId,
                role = "user",
                content = "🎨 **طلب إنشاء صورة:** $prompt"
            )
            repository.saveMessage(userMsg)

            _isAiLoading.value = true

            // Generate image via high performance Pollinations / Flux API URL
            val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
            val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&nologo=true&seed=${System.currentTimeMillis()}"

            val aiResponse = ChatMessage(
                sessionId = sessionId,
                role = "assistant",
                content = "🖼️ **تم إنشاء الصورة بنجاح!**\n\nإليك الصورة الناتجة بناءً على الوصف: *\"$prompt\"*\n\n![Generated Image]($imageUrl)",
                modelName = "Pollinations FLUX.1",
                imageUri = imageUrl,
                latencyMs = 1200
            )

            repository.saveMessage(aiResponse)
            _isAiLoading.value = false
        }
    }
}

class ChatViewModelFactory(
    private val application: Application,
    private val repository: ChatRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

