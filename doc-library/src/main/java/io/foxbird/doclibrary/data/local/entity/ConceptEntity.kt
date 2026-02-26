package io.foxbird.doclibrary.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "concepts",
    indices = [Index("normalized_name")]
)
data class ConceptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    val type: String = "concept",
    val definition: String? = null,
    @ColumnInfo(name = "document_ids_json") val documentIdsJson: String = "[]",
    @ColumnInfo(name = "chunk_ids_json") val chunkIdsJson: String = "[]",
    val frequency: Int = 1,
    @ColumnInfo(name = "related_concept_ids_json") val relatedConceptIdsJson: String? = null,
    val subject: String? = null,
    val importance: Double = 0.5,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long? = null
)
