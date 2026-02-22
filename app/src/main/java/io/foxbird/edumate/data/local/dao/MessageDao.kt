package io.foxbird.edumate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.foxbird.edumate.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>): List<Long>

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY sequence_index ASC")
    fun getByConversationFlow(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY sequence_index ASC")
    suspend fun getByConversation(conversationId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY sequence_index DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: Long, limit: Int): List<MessageEntity>

    @Query("DELETE FROM messages WHERE conversation_id = :conversationId")
    suspend fun deleteByConversation(conversationId: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE conversation_id = :conversationId")
    suspend fun getCountByConversation(conversationId: Long): Int
}
