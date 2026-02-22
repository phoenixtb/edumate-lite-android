package io.foxbird.edumate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.foxbird.edumate.data.local.entity.ChunkEntity

@Dao
interface ChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: ChunkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<ChunkEntity>): List<Long>

    @Query("SELECT * FROM chunks WHERE id = :id")
    suspend fun getById(id: Long): ChunkEntity?

    @Query("SELECT * FROM chunks WHERE material_id = :materialId ORDER BY sequence_index ASC")
    suspend fun getByMaterialId(materialId: Long): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE material_id IN (:materialIds)")
    suspend fun getByMaterialIds(materialIds: List<Long>): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE embedding IS NOT NULL AND material_id = :materialId")
    suspend fun getEmbeddedChunksByMaterial(materialId: Long): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE embedding IS NOT NULL AND material_id IN (:materialIds)")
    suspend fun getEmbeddedChunksByMaterials(materialIds: List<Long>): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE embedding IS NOT NULL")
    suspend fun getAllEmbeddedChunks(): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ChunkEntity>

    @Query("DELETE FROM chunks WHERE material_id = :materialId")
    suspend fun deleteByMaterialId(materialId: Long)

    @Query("SELECT COUNT(*) FROM chunks WHERE material_id = :materialId")
    suspend fun getCountByMaterial(materialId: Long): Int

    @Query("SELECT COUNT(*) FROM chunks")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM chunks ORDER BY id ASC")
    suspend fun getAll(): List<ChunkEntity>
}
