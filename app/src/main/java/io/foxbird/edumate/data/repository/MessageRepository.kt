package io.foxbird.edumate.data.repository

import io.foxbird.edumate.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun insert(message: MessageEntity): Long
    fun getByConversationFlow(conversationId: Long): Flow<List<MessageEntity>>
    suspend fun getRecentMessages(conversationId: Long, limit: Int): List<MessageEntity>
    suspend fun getCountByConversation(conversationId: Long): Int
}
