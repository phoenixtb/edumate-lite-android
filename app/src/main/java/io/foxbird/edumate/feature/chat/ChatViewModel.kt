package io.foxbird.edumate.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.doclibrary.domain.rag.IRagEngine
import io.foxbird.doclibrary.domain.rag.SearchResult
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.core.util.AppConstants
import io.foxbird.edumate.data.local.entity.MessageEntity
import io.foxbird.edumate.domain.service.ConversationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long = 0,
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val sourceChunks: List<SearchResult> = emptyList()
)

data class ChatUiState(
    val conversationId: Long = -1L,
    val title: String = "New Chat",
    val isGenerating: Boolean = false,
    val error: String? = null,
    val documentFilterIds: List<Long> = emptyList()
)

class ChatViewModel(
    private val conversationManager: ConversationManager,
    private val ragEngine: IRagEngine
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    fun initConversation(conversationId: Long) {
        viewModelScope.launch {
            if (conversationId == -1L) {
                val newId = conversationManager.createConversation()
                _uiState.value = _uiState.value.copy(conversationId = newId, title = "New Chat")
            } else {
                val conv = conversationManager.getConversation(conversationId)
                _uiState.value = _uiState.value.copy(
                    conversationId = conversationId,
                    title = conv?.title ?: "Chat"
                )
                loadMessages(conversationId)
            }
        }
    }

    private suspend fun loadMessages(conversationId: Long) {
        conversationManager.getMessages(conversationId).collect { msgs ->
            _messages.value = msgs.map { it.toChatMessage() }
        }
    }

    fun sendMessage(content: String) {
        val convId = _uiState.value.conversationId
        if (convId == -1L || content.isBlank()) return

        generateJob = viewModelScope.launch {
            try {
                conversationManager.addMessage(convId, "user", content)
                _messages.value = _messages.value + ChatMessage(role = "user", content = content)
                _uiState.value = _uiState.value.copy(isGenerating = true, error = null)

                if (_messages.value.size == 1) {
                    conversationManager.autoGenerateTitle(convId, content)
                }

                val history = conversationManager.getConversationHistory(convId)
                val documentIds = _uiState.value.documentFilterIds.ifEmpty { null }
                val ragContext = ragEngine.retrieve(
                    query = content,
                    documentIds = documentIds,
                    topK = AppConstants.RETRIEVAL_TOP_K,
                    threshold = AppConstants.SIMILARITY_THRESHOLD
                )

                val streamingMsg = ChatMessage(
                    role = "assistant",
                    content = "",
                    isStreaming = true,
                    sourceChunks = ragContext.chunks
                )
                _messages.value = _messages.value + streamingMsg

                val responseBuilder = StringBuilder()
                ragEngine.generateStream(
                    query = content,
                    context = ragContext,
                    conversationHistory = history,
                    maxTokens = AppConstants.MAX_INFERENCE_TOKENS,
                    temperature = AppConstants.INFERENCE_TEMPERATURE
                ).collect { token ->
                    responseBuilder.append(token)
                    _messages.value = _messages.value.dropLast(1) +
                        streamingMsg.copy(content = responseBuilder.toString())
                }

                val finalContent = responseBuilder.toString()
                val chunkIdsStr = ragContext.chunks.map { it.chunk.id }.joinToString(",")
                conversationManager.addMessage(convId, "assistant", finalContent, retrievedChunkIds = chunkIdsStr)

                _messages.value = _messages.value.dropLast(1) +
                    streamingMsg.copy(content = finalContent, isStreaming = false)
                _uiState.value = _uiState.value.copy(isGenerating = false)

            } catch (e: Exception) {
                Logger.e(TAG, "Generation failed", e)
                _uiState.value = _uiState.value.copy(isGenerating = false, error = e.message)
            }
        }
    }

    fun cancelGeneration() {
        generateJob?.cancel()
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    fun clearChat() {
        val convId = _uiState.value.conversationId
        if (convId == -1L) return
        viewModelScope.launch {
            conversationManager.deleteConversation(convId)
            val newId = conversationManager.createConversation()
            _uiState.value = _uiState.value.copy(conversationId = newId, title = "New Chat")
            _messages.value = emptyList()
        }
    }

    fun setDocumentFilter(ids: List<Long>) {
        _uiState.value = _uiState.value.copy(documentFilterIds = ids)
    }

    private fun MessageEntity.toChatMessage() = ChatMessage(id = id, role = role, content = content)
}
