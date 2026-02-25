package io.foxbird.edumate.data.repository

import io.foxbird.edumate.data.local.dao.ConversationDao
import io.foxbird.edumate.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

class RoomConversationRepository(private val dao: ConversationDao) : ConversationRepository {
    override suspend fun insert(conversation: ConversationEntity): Long = dao.insert(conversation)
    override suspend fun update(conversation: ConversationEntity) = dao.update(conversation)
    override suspend fun getById(id: Long): ConversationEntity? = dao.getById(id)
    override fun getAllFlow(): Flow<List<ConversationEntity>> = dao.getAllFlow()
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)
    override suspend fun updateTitle(id: Long, title: String) = dao.updateTitle(id, title)
    override suspend fun updateMessageCount(id: Long, count: Int, updatedAt: Long) =
        dao.updateMessageCount(id, count, updatedAt)
    override suspend fun getCount(): Int = dao.getCount()
}
