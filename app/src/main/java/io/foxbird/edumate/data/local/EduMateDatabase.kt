package io.foxbird.edumate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.foxbird.edumate.data.local.dao.ConversationDao
import io.foxbird.edumate.data.local.dao.MessageDao
import io.foxbird.edumate.data.local.entity.ConversationEntity
import io.foxbird.edumate.data.local.entity.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EduMateDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
