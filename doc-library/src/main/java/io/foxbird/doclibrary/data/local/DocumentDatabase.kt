package io.foxbird.doclibrary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DocumentDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun chunkDao(): ChunkDao
    abstract fun pageDao(): PageDao
    abstract fun conceptDao(): ConceptDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN embedded_chunk_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE documents ADD COLUMN concept_extraction_pending INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
