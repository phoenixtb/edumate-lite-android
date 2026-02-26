package io.foxbird.doclibrary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.foxbird.doclibrary.data.local.entity.ChunkEntity

@Dao
interface ChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: ChunkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<ChunkEntity>): List<Long>

    @Query("SELECT * FROM chunks WHERE id = :id")
    suspend fun getById(id: Long): ChunkEntity?

    @Query("SELECT * FROM chunks WHERE document_id = :documentId ORDER BY sequence_index ASC")
    suspend fun getByDocumentId(documentId: Long): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE document_id IN (:documentIds)")
    suspend fun getByDocumentIds(documentIds: List<Long>): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE embedding IS NOT NULL AND document_id = :documentId")
    suspend fun getEmbeddedChunksByDocument(documentId: Long): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE embedding IS NOT NULL AND document_id IN (:documentIds)")
    suspend fun getEmbeddedChunksByDocuments(documentIds: List<Long>): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE embedding IS NOT NULL")
    suspend fun getAllEmbeddedChunks(): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ChunkEntity>

    @Query("DELETE FROM chunks WHERE document_id = :documentId")
    suspend fun deleteByDocumentId(documentId: Long)

    @Query("SELECT COUNT(*) FROM chunks WHERE document_id = :documentId")
    suspend fun getCountByDocument(documentId: Long): Int

    @Query("SELECT COUNT(*) FROM chunks")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM chunks ORDER BY id ASC")
    suspend fun getAll(): List<ChunkEntity>
}
