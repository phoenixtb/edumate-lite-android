package io.foxbird.edumate.data.repository

import io.foxbird.edumate.data.local.entity.MaterialEntity
import kotlinx.coroutines.flow.Flow

interface MaterialRepository {
    suspend fun insert(material: MaterialEntity): Long
    suspend fun update(material: MaterialEntity)
    suspend fun deleteById(id: Long)
    fun getAllFlow(): Flow<List<MaterialEntity>>
    suspend fun getAll(): List<MaterialEntity>
    suspend fun getByStatus(status: String): List<MaterialEntity>
    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null)
    suspend fun updateChunkCount(id: Long, count: Int)
    suspend fun getById(id: Long): MaterialEntity?
    suspend fun getCount(): Int
    suspend fun getCompletedCount(): Int
    suspend fun getRecent(limit: Int): List<MaterialEntity>
}
