package io.foxbird.doclibrary.data.repository

import io.foxbird.doclibrary.data.local.entity.ChunkEntity

interface ChunkRepository {
    suspend fun insertAll(chunks: List<ChunkEntity>): List<Long>
    suspend fun getByDocumentId(documentId: Long): List<ChunkEntity>
    suspend fun getEmbeddedChunksByDocuments(documentIds: List<Long>): List<ChunkEntity>
    suspend fun getAllEmbeddedChunks(): List<ChunkEntity>
    suspend fun getByIds(ids: List<Long>): List<ChunkEntity>
    suspend fun deleteByDocumentId(documentId: Long)
    suspend fun getCountByDocument(documentId: Long): Int
    suspend fun getTotalCount(): Int
}
