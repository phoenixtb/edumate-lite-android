package io.foxbird.edumate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = MaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["material_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("material_id")]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "material_id") val materialId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "message_count") val messageCount: Int = 0
)
