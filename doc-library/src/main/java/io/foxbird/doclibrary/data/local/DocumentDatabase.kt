package io.foxbird.doclibrary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.foxbird.doclibrary.data.local.converter.Converters
import io.foxbird.doclibrary.data.local.dao.ChunkDao
import io.foxbird.doclibrary.data.local.dao.ConceptDao
import io.foxbird.doclibrary.data.local.dao.DocumentDao
import io.foxbird.doclibrary.data.local.dao.PageDao
import io.foxbird.doclibrary.data.local.entity.ChunkEntity
import io.foxbird.doclibrary.data.local.entity.ConceptEntity
import io.foxbird.doclibrary.data.local.entity.DocumentEntity
import io.foxbird.doclibrary.data.local.entity.PageEntity

@Database(
    entities = [
        DocumentEntity::class,
        ChunkEntity::class,
        PageEntity::class,
        ConceptEntity::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DocumentDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun chunkDao(): ChunkDao
    abstract fun pageDao(): PageDao
    abstract fun conceptDao(): ConceptDao
}
