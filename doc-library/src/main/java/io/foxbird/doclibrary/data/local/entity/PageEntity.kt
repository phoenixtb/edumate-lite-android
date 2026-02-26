package io.foxbird.doclibrary.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("document_id")]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "document_id") val documentId: Long,
    @ColumnInfo(name = "page_number") val pageNumber: Int,
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
    val width: Double? = null,
    val height: Double? = null,
    @ColumnInfo(name = "extraction_method") val extractionMethod: String = "text",
    @ColumnInfo(name = "text_density") val textDensity: Double = 0.0,
    @ColumnInfo(name = "has_equations") val hasEquations: Boolean = false,
    @ColumnInfo(name = "has_diagrams") val hasDiagrams: Boolean = false,
    @ColumnInfo(name = "has_tables") val hasTables: Boolean = false,
    @ColumnInfo(name = "has_code") val hasCode: Boolean = false,
    val summary: String? = null,
    @ColumnInfo(name = "chunk_count") val chunkCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
