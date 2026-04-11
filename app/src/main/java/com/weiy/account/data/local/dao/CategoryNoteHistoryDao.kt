package com.weiy.account.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.weiy.account.data.local.entity.CategoryNoteHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryNoteHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: CategoryNoteHistoryEntity): Long

    @Update
    suspend fun updateHistory(entity: CategoryNoteHistoryEntity)

    @Query(
        """
        SELECT *
        FROM category_note_history
        WHERE categoryId = :categoryId
        ORDER BY usageCount DESC, lastUsedAt DESC, id DESC
        """
    )
    fun observeHistoriesByCategoryId(categoryId: Long): Flow<List<CategoryNoteHistoryEntity>>

    @Query(
        """
        SELECT *
        FROM category_note_history
        ORDER BY categoryId ASC, usageCount DESC, lastUsedAt DESC, id ASC
        """
    )
    suspend fun getAllHistories(): List<CategoryNoteHistoryEntity>

    @Query(
        """
        SELECT *
        FROM category_note_history
        WHERE categoryId = :categoryId AND note = :note
        LIMIT 1
        """
    )
    suspend fun getHistoryByCategoryIdAndNote(
        categoryId: Long,
        note: String
    ): CategoryNoteHistoryEntity?

    @Query(
        """
        DELETE FROM category_note_history
        WHERE categoryId = :categoryId AND note = :note
        """
    )
    suspend fun deleteHistoryByCategoryIdAndNote(
        categoryId: Long,
        note: String
    )
}
