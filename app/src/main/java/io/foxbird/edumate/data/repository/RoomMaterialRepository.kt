package io.foxbird.edumate.data.repository

import io.foxbird.edumate.data.local.dao.MaterialDao
import io.foxbird.edumate.data.local.entity.MaterialEntity
import kotlinx.coroutines.flow.Flow

class RoomMaterialRepository(private val dao: MaterialDao) : MaterialRepository {
    override suspend fun insert(material: MaterialEntity): Long = dao.insert(material)
    override suspend fun update(material: MaterialEntity) = dao.update(material)
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override fun getAllFlow(): Flow<List<MaterialEntity>> = dao.getAllFlow()
    override suspend fun getAll(): List<MaterialEntity> = dao.getAll()
    override suspend fun getByStatus(status: String): List<MaterialEntity> = dao.getByStatus(status)
    override suspend fun updateStatus(id: Long, status: String, errorMessage: String?) =
        dao.updateStatus(id, status, errorMessage)
    override suspend fun updateChunkCount(id: Long, count: Int) = dao.updateChunkCount(id, count)
    override suspend fun getById(id: Long): MaterialEntity? = dao.getById(id)
    override suspend fun getCount(): Int = dao.getCount()
    override suspend fun getCompletedCount(): Int = dao.getCompletedCount()
    override suspend fun getRecent(limit: Int): List<MaterialEntity> = dao.getRecent(limit)
}
