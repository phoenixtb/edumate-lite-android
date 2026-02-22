package io.foxbird.edumate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.foxbird.edumate.data.local.entity.ConceptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConceptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(concept: ConceptEntity): Long

    @Update
    suspend fun update(concept: ConceptEntity)

    @Query("SELECT * FROM concepts WHERE id = :id")
    suspend fun getById(id: Long): ConceptEntity?

    @Query("SELECT * FROM concepts WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun getByNormalizedName(normalizedName: String): ConceptEntity?

    @Query("SELECT * FROM concepts WHERE type != 'keyword' ORDER BY importance DESC")
    fun getAllDisplayableFlow(): Flow<List<ConceptEntity>>

    @Query("SELECT * FROM concepts WHERE type != 'keyword' ORDER BY importance DESC")
    suspend fun getAllDisplayable(): List<ConceptEntity>

    @Query("SELECT * FROM concepts ORDER BY importance DESC")
    suspend fun getAll(): List<ConceptEntity>

    @Query("DELETE FROM concepts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM concepts WHERE type != 'keyword'")
    suspend fun getDisplayableCount(): Int

    @Query("SELECT * FROM concepts WHERE material_ids_json LIKE '%' || :materialId || '%' AND type != 'keyword' ORDER BY importance DESC")
    suspend fun getByMaterialId(materialId: Long): List<ConceptEntity>
}
