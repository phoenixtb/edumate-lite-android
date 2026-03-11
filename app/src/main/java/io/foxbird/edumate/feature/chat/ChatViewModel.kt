package io.foxbird.edumate.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.doclibrary.data.repository.DocumentRepository
import io.foxbird.doclibrary.domain.rag.IRagEngine
import io.foxbird.doclibrary.domain.rag.SearchResult
import io.foxbird.edgeai.agent.AgentResult
import io.foxbird.edgeai.agent.AgentStep
import io.foxbird.edgeai.agent.IAgentOrchestrator
import io.foxbird.edgeai.engine.ModelManager
import io.foxbird.edgeai.engine.StreamEvent
import io.foxbird.edgeai.engine.parseThinkTags
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.core.util.AppConstants
import io.foxbird.edumate.data.local.entity.MessageEntity
import io.foxbird.edumate.domain.service.ConversationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long = 0,
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val confidenceScore: Float? = null,
    val sourceChunks: List<SearchResult> = emptyList(),
    val agentSteps: List<AgentStepDisplay> = emptyList(),
    /** Accumulated chain-of-thought text from the model's think tags. Null = no thinking mode. */
    val thinkingContent: String? = null,
    /** True while the model is currently inside a think block (i.e. still reasoning). */
    val isThinking: Boolean = false
)

data class AgentStepDisplay(
    val label: String,
    val detail: String,
    val type: AgentStepType
)

enum class AgentStepType { TOOL_CALL, TOOL_RESULT, THINKING }

data class ChatUiState(
    val conversationId: Long = -1L,
    val title: String = "New Chat",
    val isGenerating: Boolean = false,
    val isAgentMode: Boolean = false,
    val isModelReady: Boolean = false,
    val error: String? = null,
    val documentFilterIds: List<Long> = emptyList(),
    val agentModeOffBanner: Boolean = false
)

class ChatViewModel(
    private val conversationManager: ConversationManager,
    private val ragEngine: IRagEngine,
    private val documentRepository: DocumentRepository,
    private val modelManager: ModelManager,
    private val agentOrchestrator: IAgentOrchestrator? = null
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

    fun toggleAgentMode() {
        val newAgentMode = !_uiState.value.isAgentMode
        if (newAgentMode) {
            _uiState.value = _uiState.value.copy(
                isAgentMode = true,
                isModelReady = agentOrchestrator?.isReady() == true,
                agentModeOffBanner = false
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isAgentMode = false,
                isModelReady = false,
                agentModeOffBanner = true
            )
            viewModelScope.launch {
                delay(3000)
                _uiState.value = _uiState.value.copy(agentModeOffBanner = false)
            }
        }
    }

    fun sendMessage(content: String) {
        if (_uiState.value.isAgentMode && agentOrchestrator != null) {
            sendAgentMessage(content)
        } else {
            sendRagMessage(content)
        }
    }

    // -------------------------------------------------------------------------
    // Standard RAG mode
    // -------------------------------------------------------------------------

    private fun sendRagMessage(content: String) {
        val convId = _uiState.value.conversationId
        if (convId == -1L || content.isBlank()) return

        generateJob = viewModelScope.launch {
            try {
                conversationManager.addMessage(convId, "user", content)
                _messages.value = _messages.value + ChatMessage(role = "user", content = content)
                _uiState.value = _uiState.value.copy(isGenerating = true, error = null)

                if (_messages.value.count { it.role == "user" } == 1) {
                    conversationManager.autoGenerateTitle(convId, content)
                }

                runRagGeneration(content, convId)

            } catch (e: Exception) {
                Logger.e(TAG, "Generation failed", e)
                _uiState.value = _uiState.value.copy(isGenerating = false, error = e.message)
                if (_messages.value.lastOrNull()?.isStreaming == true) {
                    _messages.value = _messages.value.dropLast(1)
                }
            }
        }
    }

    private suspend fun runRagGeneration(content: String, convId: Long) {
        // Add placeholder immediately so thinking dots appear during retrieve()
        val placeholder = ChatMessage(role = "assistant", content = "", isStreaming = true)
        _messages.value = _messages.value + placeholder

        val documentIds = _uiState.value.documentFilterIds.ifEmpty { null }
        val ragContext = ragEngine.retrieve(
            query = content,
            documentIds = documentIds,
            topK = AppConstants.RETRIEVAL_TOP_K,
            threshold = AppConstants.SIMILARITY_THRESHOLD
        )

        // Compute confidence as average hybrid score across retrieved chunks
        val confidence = ragContext.chunks
            .takeIf { it.isNotEmpty() }
            ?.map { it.score }
            ?.average()
            ?.toFloat()

        // Enrich chunks with document title for sources panel
        val enrichedChunks = ragContext.chunks.map { result ->
            val title = runCatching { documentRepository.getById(result.chunk.documentId)?.title }.getOrNull()
            result.copy(documentTitle = title)
        }

        val history = conversationManager.getConversationHistory(convId)

        // Resolve think tags for the active model (null if model has no thinking mode)
        val activeModelConfig = modelManager.getActiveInferenceConfig()
        val thinkOpen = activeModelConfig?.thinkOpenTag
        val thinkClose = activeModelConfig?.thinkCloseTag

        val streamingMsg = ChatMessage(
            role = "assistant",
            content = "",
            isStreaming = true,
            confidenceScore = confidence,
            sourceChunks = enrichedChunks,
            thinkingContent = if (thinkOpen != null) "" else null
        )
        // Replace placeholder with enriched streaming message
        _messages.value = _messages.value.dropLast(1) + streamingMsg

        val responseBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()

        ragEngine.generateStream(
            query = content,
            context = ragContext,
            conversationHistory = history,
            maxTokens = AppConstants.MAX_INFERENCE_TOKENS,
            temperature = AppConstants.INFERENCE_TEMPERATURE
        ).parseThinkTags(thinkOpen, thinkClose).collect { event ->
            when (event) {
                is StreamEvent.ThinkingToken -> {
                    thinkingBuilder.append(event.text)
                    _messages.value = _messages.value.dropLast(1) +
                        streamingMsg.copy(
                            content = responseBuilder.toString(),
                            thinkingContent = thinkingBuilder.toString(),
                            isThinking = true
                        )
                }
                is StreamEvent.ContentToken -> {
                    responseBuilder.append(event.text)
                    _messages.value = _messages.value.dropLast(1) +
                        streamingMsg.copy(
                            content = responseBuilder.toString(),
                            thinkingContent = thinkingBuilder.toString().takeIf { thinkOpen != null },
                            isThinking = false
                        )
                }
            }
        }

        val finalContent = responseBuilder.toString()
        val chunkIdsStr = enrichedChunks.map { it.chunk.id }.joinToString(",")
        conversationManager.addMessage(convId, "assistant", finalContent, retrievedChunkIds = chunkIdsStr)

        _messages.value = _messages.value.dropLast(1) +
            streamingMsg.copy(
                content = finalContent,
                isStreaming = false,
                thinkingContent = thinkingBuilder.toString().takeIf { thinkOpen != null },
                isThinking = false
            )
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    fun regenerateLastResponse() {
        if (_uiState.value.isGenerating) return
        val lastUserContent = _messages.value.lastOrNull { it.role == "user" }?.content ?: return
        val convId = _uiState.value.conversationId
        if (convId == -1L) return

        // Drop last assistant message from UI (keep DB as-is; a new response will be saved)
        if (_messages.value.lastOrNull()?.role == "assistant") {
            _messages.value = _messages.value.dropLast(1)
        }

        generateJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
                runRagGeneration(lastUserContent, convId)
            } catch (e: Exception) {
                Logger.e(TAG, "Regeneration failed", e)
                _uiState.value = _uiState.value.copy(isGenerating = false, error = e.message)
                if (_messages.value.lastOrNull()?.isStreaming == true) {
                    _messages.value = _messages.value.dropLast(1)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Agent mode
    // -------------------------------------------------------------------------

    private fun sendAgentMessage(content: String) {
        val convId = _uiState.value.conversationId
        val orchestrator = agentOrchestrator ?: return
        if (convId == -1L || content.isBlank()) return

        generateJob = viewModelScope.launch {
            try {
                conversationManager.addMessage(convId, "user", content)
                _messages.value = _messages.value + ChatMessage(role = "user", content = content)
                _uiState.value = _uiState.value.copy(isGenerating = true, error = null)

                if (_messages.value.count { it.role == "user" } == 1) {
                    conversationManager.autoGenerateTitle(convId, content)
                }

                val agentStepsAccumulated = mutableListOf<AgentStepDisplay>()

                val placeholderMsg = ChatMessage(role = "assistant", content = "", isStreaming = true)
                _messages.value = _messages.value + placeholderMsg

                val stepsJob = launch {
                    orchestrator.agentSteps.collect { step ->
                        val display = step.toDisplay()
                        agentStepsAccumulated.add(display)
                        val placeholder = _messages.value.lastOrNull()
                        if (placeholder?.role == "assistant") {
                            _messages.value = _messages.value.dropLast(1) +
                                placeholder.copy(agentSteps = agentStepsAccumulated.toList())
                        }
                    }
                }

                val result = orchestrator.runAgent(content, emptyList())
                stepsJob.cancel()

                val finalContent = when (result) {
                    is AgentResult.Success -> result.output
                    is AgentResult.Failure -> when {
                        result.reason.contains("finish in given number of steps", ignoreCase = true) ->
                            "I ran out of steps before finishing. Try a more specific question or increase the step limit."
                        else -> "I couldn't complete that: ${result.reason}"
                    }
                }

                conversationManager.addMessage(convId, "assistant", finalContent)
                _messages.value = _messages.value.dropLast(1) +
                    placeholderMsg.copy(
                        content = finalContent,
                        isStreaming = false,
                        agentSteps = agentStepsAccumulated.toList()
                    )
                _uiState.value = _uiState.value.copy(isGenerating = false)

            } catch (e: Exception) {
                Logger.e(TAG, "Agent run failed", e)
                _uiState.value = _uiState.value.copy(isGenerating = false, error = e.message)
                if (_messages.value.lastOrNull()?.isStreaming == true) {
                    _messages.value = _messages.value.dropLast(1)
                }
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

    private fun AgentStep.toDisplay(): AgentStepDisplay = when (this) {
        is AgentStep.ToolCalling -> AgentStepDisplay(
            label = "Using $toolName",
            detail = args.cleanStepText(80),
            type = AgentStepType.TOOL_CALL
        )
        is AgentStep.ToolResult -> AgentStepDisplay(
            label = "$toolName result",
            detail = result.cleanStepText(120),
            type = AgentStepType.TOOL_RESULT
        )
        is AgentStep.Thinking -> AgentStepDisplay(
            label = "Reasoning",
            detail = text.cleanStepText(120),
            type = AgentStepType.THINKING
        )
    }

    private fun String.cleanStepText(limit: Int): String =
        this.replace("\\n", "\n").replace("\\t", " ").trim().take(limit)
}
