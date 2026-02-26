package io.foxbird.doclibrary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.foxbird.doclibrary.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity): Long

    @Update
    suspend fun update(document: DocumentEntity)

    @Delete
    suspend fun delete(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY created_at DESC")
    suspend fun getAll(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE status = :status ORDER BY created_at DESC")
    suspend fun getByStatus(status: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE subject = :subject ORDER BY created_at DESC")
    suspend fun getBySubject(subject: String): List<DocumentEntity>

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM documents WHERE status = 'completed'")
    suspend fun getCompletedCount(): Int

    @Query("UPDATE documents SET last_accessed_at = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: Long, timestamp: Long)

    @Query("UPDATE documents SET status = :status, error_message = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null)

    @Query("UPDATE documents SET chunk_count = :count WHERE id = :id")
    suspend fun updateChunkCount(id: Long, count: Int)

    @Query("UPDATE documents SET title = :title, subject = :subject, grade_level = :gradeLevel WHERE id = :id")
    suspend fun updateMetadata(id: Long, title: String, subject: String?, gradeLevel: Int?)

    @Query("SELECT * FROM documents WHERE status = 'completed' ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DocumentEntity>
}
