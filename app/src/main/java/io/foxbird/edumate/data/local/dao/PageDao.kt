package io.foxbird.edumate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.foxbird.edumate.data.local.entity.PageEntity

@Dao
interface PageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: PageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<PageEntity>): List<Long>

    @Query("SELECT * FROM pages WHERE material_id = :materialId ORDER BY page_number ASC")
    suspend fun getByMaterialId(materialId: Long): List<PageEntity>

    @Query("DELETE FROM pages WHERE material_id = :materialId")
    suspend fun deleteByMaterialId(materialId: Long)

    @Query("SELECT COUNT(*) FROM pages WHERE material_id = :materialId")
    suspend fun getCountByMaterial(materialId: Long): Int
}
