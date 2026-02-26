package io.foxbird.doclibrary.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "original_file_path") val originalFilePath: String? = null,
    @ColumnInfo(name = "source_type") val sourceType: String,
    val subject: String? = null,
    @ColumnInfo(name = "grade_level") val gradeLevel: Int? = null,
    val status: String = "pending",
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "processed_at") val processedAt: Long? = null,
    @ColumnInfo(name = "last_accessed_at") val lastAccessedAt: Long? = null,
    @ColumnInfo(name = "chunk_count") val chunkCount: Int = 0,
    @ColumnInfo(name = "processing_mode") val processingMode: String = "fast",
    @ColumnInfo(name = "page_count") val pageCount: Int = 0,
    @ColumnInfo(name = "extraction_quality") val extractionQuality: Double = 0.0,
    @ColumnInfo(name = "file_hash") val fileHash: String? = null,
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Int? = null,
    @ColumnInfo(name = "total_tokens") val totalTokens: Int = 0,
    @ColumnInfo(name = "total_words") val totalWords: Int = 0,
    val language: String? = null,
    @ColumnInfo(name = "detected_topics_json") val detectedTopicsJson: String? = null,
    @ColumnInfo(name = "keywords_json") val keywordsJson: String? = null
)
