package io.foxbird.edumate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.foxbird.edumate.data.local.entity.MaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(material: MaterialEntity): Long

    @Update
    suspend fun update(material: MaterialEntity)

    @Delete
    suspend fun delete(material: MaterialEntity)

    @Query("DELETE FROM materials WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM materials WHERE id = :id")
    suspend fun getById(id: Long): MaterialEntity?

    @Query("SELECT * FROM materials ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials ORDER BY created_at DESC")
    suspend fun getAll(): List<MaterialEntity>

    @Query("SELECT * FROM materials WHERE status = :status ORDER BY created_at DESC")
    suspend fun getByStatus(status: String): List<MaterialEntity>

    @Query("SELECT * FROM materials WHERE subject = :subject ORDER BY created_at DESC")
    suspend fun getBySubject(subject: String): List<MaterialEntity>

    @Query("SELECT COUNT(*) FROM materials")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM materials WHERE status = 'completed'")
    suspend fun getCompletedCount(): Int

    @Query("UPDATE materials SET last_accessed_at = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: Long, timestamp: Long)

    @Query("UPDATE materials SET status = :status, error_message = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null)

    @Query("UPDATE materials SET chunk_count = :count WHERE id = :id")
    suspend fun updateChunkCount(id: Long, count: Int)

    @Query("SELECT * FROM materials WHERE status = 'completed' ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<MaterialEntity>
}
