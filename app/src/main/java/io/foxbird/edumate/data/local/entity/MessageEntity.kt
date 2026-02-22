package io.foxbird.edumate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversation_id")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "conversation_id") val conversationId: Long,
    val role: String,
    val content: String,
    @ColumnInfo(name = "retrieved_chunk_ids") val retrievedChunkIds: String? = null,
    @ColumnInfo(name = "confidence_score") val confidenceScore: Double? = null,
    val timestamp: Long,
    @ColumnInfo(name = "sequence_index") val sequenceIndex: Int = 0
)
