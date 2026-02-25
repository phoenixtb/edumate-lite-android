package io.foxbird.edumate.data.repository

import io.foxbird.edumate.data.local.dao.MessageDao
import io.foxbird.edumate.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

class RoomMessageRepository(private val dao: MessageDao) : MessageRepository {
    override suspend fun insert(message: MessageEntity): Long = dao.insert(message)
    override fun getByConversationFlow(conversationId: Long): Flow<List<MessageEntity>> =
        dao.getByConversationFlow(conversationId)
    override suspend fun getRecentMessages(conversationId: Long, limit: Int): List<MessageEntity> =
        dao.getRecentMessages(conversationId, limit)
    override suspend fun getCountByConversation(conversationId: Long): Int =
        dao.getCountByConversation(conversationId)
}
