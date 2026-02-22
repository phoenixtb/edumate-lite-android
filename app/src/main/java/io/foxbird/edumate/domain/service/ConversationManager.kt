package io.foxbird.edumate.domain.service

import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.GenerationParams
import io.foxbird.edgeai.util.Logger
import io.foxbird.edumate.core.util.AppConstants
import io.foxbird.edumate.data.local.dao.ConversationDao
import io.foxbird.edumate.data.local.dao.MessageDao
import io.foxbird.edumate.data.local.entity.ConversationEntity
import io.foxbird.edumate.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

class ConversationManager(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val orchestrator: EngineOrchestrator
) {
    companion object {
        private const val TAG = "ConversationManager"
    }

    fun getAllConversations(): Flow<List<ConversationEntity>> = conversationDao.getAllFlow()

    suspend fun getConversation(id: Long): ConversationEntity? = conversationDao.getById(id)

    fun getMessages(conversationId: Long): Flow<List<MessageEntity>> =
        messageDao.getByConversationFlow(conversationId)

    suspend fun createConversation(
        title: String = "New Chat",
        materialId: Long? = null
    ): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insert(
            ConversationEntity(
                title = title,
                materialId = materialId,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun addMessage(
        conversationId: Long,
        role: String,
        content: String,
        retrievedChunkIds: String? = null,
        confidenceScore: Double? = null
    ): Long {
        val messageCount = messageDao.getCountByConversation(conversationId)
        val messageId = messageDao.insert(
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
        conversationDao.updateMessageCount(
            conversationId, messageCount + 1, System.currentTimeMillis()
        )
        return messageId
    }

    suspend fun getConversationHistory(conversationId: Long): String {
        val messages = messageDao.getRecentMessages(
            conversationId,
            AppConstants.MAX_CONTEXT_MESSAGES
        ).reversed()

        return messages.joinToString("\n") { msg ->
            val role = if (msg.role == "user") "User" else "Assistant"
            "$role: ${msg.content}"
        }
    }

    suspend fun autoGenerateTitle(conversationId: Long, firstMessage: String) {
        try {
            val prompt = PromptTemplates.titlePrompt(firstMessage)
            val result = orchestrator.generateComplete(
                prompt = prompt,
                params = GenerationParams(maxTokens = 20, temperature = 0.3f)
            )
            result.fold(
                ifLeft = { Logger.e(TAG, "Title gen failed: ${it.message}") },
                ifRight = { title ->
                    val cleanTitle = title.trim().take(50).trim()
                    if (cleanTitle.isNotBlank()) {
                        conversationDao.updateTitle(conversationId, cleanTitle)
                    }
                }
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Title generation failed", e)
        }
    }

    suspend fun deleteConversation(id: Long) {
        conversationDao.deleteById(id)
    }

    suspend fun deleteConversations(ids: List<Long>) {
        conversationDao.deleteByIds(ids)
    }
}
