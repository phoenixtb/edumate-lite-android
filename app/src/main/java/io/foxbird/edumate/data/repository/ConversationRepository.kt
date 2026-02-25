package io.foxbird.edumate.data.repository

import io.foxbird.edumate.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    suspend fun insert(conversation: ConversationEntity): Long
    suspend fun update(conversation: ConversationEntity)
    suspend fun getById(id: Long): ConversationEntity?
    fun getAllFlow(): Flow<List<ConversationEntity>>
    suspend fun deleteById(id: Long)
    suspend fun deleteByIds(ids: List<Long>)
    suspend fun updateTitle(id: Long, title: String)
    suspend fun updateMessageCount(id: Long, count: Int, updatedAt: Long)
    suspend fun getCount(): Int
}
