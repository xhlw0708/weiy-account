package com.weiy.account.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.weiy.account.data.local.dao.CategoryDao
import com.weiy.account.data.local.dao.CategoryNoteHistoryDao
import com.weiy.account.data.local.dao.TransactionDao
import com.weiy.account.data.local.entity.CategoryEntity
import com.weiy.account.data.local.entity.CategoryNoteHistoryEntity
import com.weiy.account.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, CategoryNoteHistoryEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun categoryNoteHistoryDao(): CategoryNoteHistoryDao

    companion object {
        private const val DATABASE_NAME = "weiy_account.db"
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `category_note_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `usageCount` INTEGER NOT NULL,
                        `lastUsedAt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_category_note_history_categoryId`
                    ON `category_note_history` (`categoryId`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_category_note_history_categoryId_note`
                    ON `category_note_history` (`categoryId`, `note`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO category_note_history (categoryId, note, usageCount, lastUsedAt, createdAt, updatedAt)
                    SELECT
                        categoryId,
                        TRIM(note) AS note,
                        COUNT(*) AS usageCount,
                        MAX(dateTime) AS lastUsedAt,
                        MIN(createdAt) AS createdAt,
                        MAX(updatedAt) AS updatedAt
                    FROM transactions
                    WHERE TRIM(note) != ''
                    GROUP BY categoryId, TRIM(note)
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
