package io.foxbird.doclibrary.data.repository

import io.foxbird.doclibrary.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    suspend fun insert(document: DocumentEntity): Long
    suspend fun update(document: DocumentEntity)
    suspend fun deleteById(id: Long)
    fun getAllFlow(): Flow<List<DocumentEntity>>
    suspend fun getAll(): List<DocumentEntity>
    suspend fun getByStatus(status: String): List<DocumentEntity>
    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null)
    suspend fun updateChunkCount(id: Long, count: Int)
    suspend fun updateMetadata(id: Long, title: String, subject: String?, gradeLevel: Int?)
    suspend fun getById(id: Long): DocumentEntity?
    suspend fun getCount(): Int
    suspend fun getCompletedCount(): Int
    suspend fun getRecent(limit: Int): List<DocumentEntity>
}
