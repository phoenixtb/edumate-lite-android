package io.foxbird.edumate.data.repository

import io.foxbird.edumate.data.local.entity.ChunkEntity

interface ChunkRepository {
    suspend fun insertAll(chunks: List<ChunkEntity>): List<Long>
    suspend fun getByMaterialId(materialId: Long): List<ChunkEntity>
    suspend fun getEmbeddedChunksByMaterials(materialIds: List<Long>): List<ChunkEntity>
    suspend fun getAllEmbeddedChunks(): List<ChunkEntity>
    suspend fun getByIds(ids: List<Long>): List<ChunkEntity>
    suspend fun deleteByMaterialId(materialId: Long)
    suspend fun getCountByMaterial(materialId: Long): Int
    suspend fun getTotalCount(): Int
}
