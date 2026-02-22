package io.foxbird.edumate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.foxbird.edumate.data.local.converter.Converters
import io.foxbird.edumate.data.local.dao.ChunkDao
import io.foxbird.edumate.data.local.dao.ConceptDao
import io.foxbird.edumate.data.local.dao.ConversationDao
import io.foxbird.edumate.data.local.dao.MaterialDao
import io.foxbird.edumate.data.local.dao.MessageDao
import io.foxbird.edumate.data.local.dao.PageDao
import io.foxbird.edumate.data.local.entity.ChunkEntity
import io.foxbird.edumate.data.local.entity.ConceptEntity
import io.foxbird.edumate.data.local.entity.ConversationEntity
import io.foxbird.edumate.data.local.entity.MaterialEntity
import io.foxbird.edumate.data.local.entity.MessageEntity
import io.foxbird.edumate.data.local.entity.PageEntity

@Database(
    entities = [
        MaterialEntity::class,
        ChunkEntity::class,
        PageEntity::class,
        ConceptEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EduMateDatabase : RoomDatabase() {
    abstract fun materialDao(): MaterialDao
    abstract fun chunkDao(): ChunkDao
    abstract fun pageDao(): PageDao
    abstract fun conceptDao(): ConceptDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
