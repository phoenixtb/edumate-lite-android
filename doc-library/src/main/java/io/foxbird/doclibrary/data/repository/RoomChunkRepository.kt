package io.foxbird.doclibrary.data.repository

import io.foxbird.doclibrary.data.local.dao.ChunkDao
import io.foxbird.doclibrary.data.local.entity.ChunkEntity

class RoomChunkRepository(private val dao: ChunkDao) : ChunkRepository {
    override suspend fun insertAll(chunks: List<ChunkEntity>): List<Long> = dao.insertAll(chunks)
    override suspend fun getByDocumentId(documentId: Long): List<ChunkEntity> = dao.getByDocumentId(documentId)
    override suspend fun getEmbeddedChunksByDocuments(documentIds: List<Long>): List<ChunkEntity> =
        dao.getEmbeddedChunksByDocuments(documentIds)
    override suspend fun getAllEmbeddedChunks(): List<ChunkEntity> = dao.getAllEmbeddedChunks()
    override suspend fun getByIds(ids: List<Long>): List<ChunkEntity> = dao.getByIds(ids)
    override suspend fun deleteByDocumentId(documentId: Long) = dao.deleteByDocumentId(documentId)
    override suspend fun getCountByDocument(documentId: Long): Int = dao.getCountByDocument(documentId)
    override suspend fun getTotalCount(): Int = dao.getTotalCount()
}
