package io.foxbird.edumate.domain.service

import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.GenerationParams
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.core.util.AppConstants
import io.foxbird.edumate.data.local.entity.ConversationEntity
import io.foxbird.edumate.data.local.entity.MessageEntity
import io.foxbird.edumate.data.repository.ConversationRepository
import io.foxbird.edumate.data.repository.MessageRepository
import kotlinx.coroutines.flow.Flow

class ConversationManager(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val orchestrator: EngineOrchestrator
) {
    companion object {
        private const val TAG = "ConversationManager"
    }

    fun getAllConversations(): Flow<List<ConversationEntity>> = conversationRepository.getAllFlow()

    suspend fun getConversation(id: Long): ConversationEntity? = conversationRepository.getById(id)

    fun getMessages(conversationId: Long): Flow<List<MessageEntity>> =
        messageRepository.getByConversationFlow(conversationId)

    suspend fun createConversation(title: String = "New Chat", documentId: Long? = null): Long {
        val now = System.currentTimeMillis()
        return conversationRepository.insert(
            ConversationEntity(title = title, documentId = documentId, createdAt = now, updatedAt = now)
        )
    }

    suspend fun addMessage(
        conversationId: Long,
        role: String,
        content: String,
        retrievedChunkIds: String? = null,
        confidenceScore: Double? = null
    ): Long {
        val messageCount = messageRepository.getCountByConversation(conversationId)
        val messageId = messageRepository.insert(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                content = content,
                retrievedChunkIds = retrievedChunkIds,
                confidenceScore = confidenceScore,
                timestamp = System.currentTimeMillis(),
                sequenceIndex = messageCount
            )
        )
        conversationRepository.updateMessageCount(conversationId, messageCount + 1, System.currentTimeMillis())
        return messageId
    }

    suspend fun getConversationHistory(conversationId: Long): String {
        val messages = messageRepository.getRecentMessages(conversationId, AppConstants.MAX_CONTEXT_MESSAGES)
            .reversed()
        return messages.joinToString("\n") { msg ->
            val role = if (msg.role == "user") "User" else "Assistant"
            "$role: ${msg.content}"
        }
    }

    suspend fun autoGenerateTitle(conversationId: Long, firstMessage: String) {
        try {
            val prompt = PromptTemplates.titlePrompt(firstMessage)
            val result = orchestrator.generateComplete(prompt = prompt, params = GenerationParams(maxTokens = 20, temperature = 0.3f))
            result.fold(
                ifLeft = { Logger.e(TAG, "Title gen failed: ${it.message}") },
                ifRight = { title ->
                    val cleanTitle = title.trim().take(50).trim()
                    if (cleanTitle.isNotBlank()) conversationRepository.updateTitle(conversationId, cleanTitle)
                }
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Title generation failed", e)
        }
    }

    suspend fun deleteConversation(id: Long) = conversationRepository.deleteById(id)

    suspend fun deleteConversations(ids: List<Long>) = conversationRepository.deleteByIds(ids)
}
