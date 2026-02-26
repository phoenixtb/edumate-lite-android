package io.foxbird.doclibrary.data.repository

import io.foxbird.doclibrary.data.local.dao.DocumentDao
import io.foxbird.doclibrary.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

class RoomDocumentRepository(private val dao: DocumentDao) : DocumentRepository {
    override suspend fun insert(document: DocumentEntity): Long = dao.insert(document)
    override suspend fun update(document: DocumentEntity) = dao.update(document)
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
    override fun getAllFlow(): Flow<List<DocumentEntity>> = dao.getAllFlow()
    override suspend fun getAll(): List<DocumentEntity> = dao.getAll()
    override suspend fun getByStatus(status: String): List<DocumentEntity> = dao.getByStatus(status)
    override suspend fun updateStatus(id: Long, status: String, errorMessage: String?) =
        dao.updateStatus(id, status, errorMessage)
    override suspend fun updateChunkCount(id: Long, count: Int) = dao.updateChunkCount(id, count)
    override suspend fun updateMetadata(id: Long, title: String, subject: String?, gradeLevel: Int?) =
        dao.updateMetadata(id, title, subject, gradeLevel)
    override suspend fun getById(id: Long): DocumentEntity? = dao.getById(id)
    override suspend fun getCount(): Int = dao.getCount()
    override suspend fun getCompletedCount(): Int = dao.getCompletedCount()
    override suspend fun getRecent(limit: Int): List<DocumentEntity> = dao.getRecent(limit)
}
