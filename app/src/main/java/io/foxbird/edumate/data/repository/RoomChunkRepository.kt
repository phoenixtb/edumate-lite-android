package io.foxbird.edumate.data.repository

import io.foxbird.edumate.data.local.dao.ChunkDao
import io.foxbird.edumate.data.local.entity.ChunkEntity

class RoomChunkRepository(private val dao: ChunkDao) : ChunkRepository {
    override suspend fun insertAll(chunks: List<ChunkEntity>): List<Long> = dao.insertAll(chunks)
    override suspend fun getByMaterialId(materialId: Long): List<ChunkEntity> = dao.getByMaterialId(materialId)
    override suspend fun getEmbeddedChunksByMaterials(materialIds: List<Long>): List<ChunkEntity> =
        dao.getEmbeddedChunksByMaterials(materialIds)
    override suspend fun getAllEmbeddedChunks(): List<ChunkEntity> = dao.getAllEmbeddedChunks()
    override suspend fun getByIds(ids: List<Long>): List<ChunkEntity> = dao.getByIds(ids)
    override suspend fun deleteByMaterialId(materialId: Long) = dao.deleteByMaterialId(materialId)
    override suspend fun getCountByMaterial(materialId: Long): Int = dao.getCountByMaterial(materialId)
    override suspend fun getTotalCount(): Int = dao.getTotalCount()
}
