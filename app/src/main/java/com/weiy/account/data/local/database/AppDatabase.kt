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
import com.weiy.account.data.local.dao.RecurringAccountingExecutionDao
import com.weiy.account.data.local.dao.RecurringAccountingRuleDao
import com.weiy.account.data.local.dao.TransactionDao
import com.weiy.account.data.local.entity.CategoryEntity
import com.weiy.account.data.local.entity.CategoryNoteHistoryEntity
import com.weiy.account.data.local.entity.RecurringAccountingExecutionEntity
import com.weiy.account.data.local.entity.RecurringAccountingRuleEntity
import com.weiy.account.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CategoryNoteHistoryEntity::class,
        RecurringAccountingRuleEntity::class,
        RecurringAccountingExecutionEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun categoryNoteHistoryDao(): CategoryNoteHistoryDao

    abstract fun recurringAccountingRuleDao(): RecurringAccountingRuleDao

    abstract fun recurringAccountingExecutionDao(): RecurringAccountingExecutionDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_accounting_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `tagsSerialized` TEXT NOT NULL,
                        `firstOccurrenceDateEpochDay` INTEGER NOT NULL,
                        `repeatUnit` TEXT NOT NULL,
                        `repeatInterval` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `lastExecutedDateEpochDay` INTEGER,
                        `nextDueDateEpochDay` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_recurring_accounting_rules_categoryId`
                    ON `recurring_accounting_rules` (`categoryId`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_recurring_accounting_rules_enabled`
                    ON `recurring_accounting_rules` (`enabled`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_recurring_accounting_rules_nextDueDateEpochDay`
                    ON `recurring_accounting_rules` (`nextDueDateEpochDay`)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_accounting_executions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ruleId` INTEGER NOT NULL,
                        `occurrenceDateEpochDay` INTEGER NOT NULL,
                        `generatedTransactionId` INTEGER,
                        `executedAt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`ruleId`) REFERENCES `recurring_accounting_rules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_recurring_accounting_executions_ruleId`
                    ON `recurring_accounting_executions` (`ruleId`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_accounting_executions_ruleId_occurrenceDateEpochDay`
                    ON `recurring_accounting_executions` (`ruleId`, `occurrenceDateEpochDay`)
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
