package io.foxbird.doclibrary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.foxbird.doclibrary.data.local.entity.PageEntity

@Dao
interface PageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: PageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<PageEntity>): List<Long>

    @Query("SELECT * FROM pages WHERE document_id = :documentId ORDER BY page_number ASC")
    suspend fun getByDocumentId(documentId: Long): List<PageEntity>

    @Query("DELETE FROM pages WHERE document_id = :documentId")
    suspend fun deleteByDocumentId(documentId: Long)

    @Query("SELECT COUNT(*) FROM pages WHERE document_id = :documentId")
    suspend fun getCountByDocument(documentId: Long): Int
}
