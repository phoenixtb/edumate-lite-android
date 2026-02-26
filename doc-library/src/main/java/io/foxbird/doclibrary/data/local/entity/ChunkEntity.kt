package io.foxbird.doclibrary.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chunks",
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
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "document_id") val documentId: Long,
    val content: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val embedding: ByteArray? = null,
    @ColumnInfo(name = "page_number") val pageNumber: Int? = null,
    @ColumnInfo(name = "section_index") val sectionIndex: Int? = null,
    @ColumnInfo(name = "sequence_index") val sequenceIndex: Int = 0,
    @ColumnInfo(name = "chunk_type") val chunkType: String = "paragraph",
    @ColumnInfo(name = "word_count") val wordCount: Int = 0,
    @ColumnInfo(name = "metadata_json") val metadataJson: String? = null,
    @ColumnInfo(name = "token_count") val tokenCount: Int = 0,
    @ColumnInfo(name = "confidence_score") val confidenceScore: Double = 1.0,
    @ColumnInfo(name = "extraction_method") val extractionMethod: String = "text",
    @ColumnInfo(name = "start_offset") val startOffset: Int? = null,
    @ColumnInfo(name = "end_offset") val endOffset: Int? = null,
    val importance: Double = 0.5,
    @ColumnInfo(name = "is_key_point") val isKeyPoint: Boolean = false,
    @ColumnInfo(name = "keywords_json") val keywordsJson: String? = null,
    @ColumnInfo(name = "entities_json") val entitiesJson: String? = null,
    @ColumnInfo(name = "concept_tags_json") val conceptTagsJson: String? = null,
    @ColumnInfo(name = "parent_chunk_id") val parentChunkId: Long? = null,
    @ColumnInfo(name = "related_chunk_ids_json") val relatedChunkIdsJson: String? = null,
    @ColumnInfo(name = "sentence_count") val sentenceCount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChunkEntity) return false
        return id == other.id && documentId == other.documentId && content == other.content
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }
}
