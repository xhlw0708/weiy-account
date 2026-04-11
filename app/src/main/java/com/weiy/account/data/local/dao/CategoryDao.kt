package com.weiy.account.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.weiy.account.data.local.entity.CategoryEntity
import com.weiy.account.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun updateCategoryName(id: Long, name: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :id")
    suspend fun countTransactionsByCategoryId(id: Long): Int

    @Query(
        """
        SELECT * FROM categories
        WHERE type = :type
        ORDER BY sortOrder ASC, id ASC
        """
    )
    fun observeCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT * FROM categories
        ORDER BY type ASC, sortOrder ASC, id ASC
        """
    )
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query(
        """
        SELECT * FROM categories
        WHERE name = :name AND type = :type
        LIMIT 1
        """
    )
    suspend fun getCategoryByNameAndType(
        name: String,
        type: TransactionType
    ): CategoryEntity?

    @Query(
        """
        SELECT * FROM categories
        WHERE type = :type
        ORDER BY sortOrder ASC, id ASC
        LIMIT 1
        """
    )
    suspend fun getFirstCategoryByType(type: TransactionType): CategoryEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories WHERE type = :type")
    suspend fun getMaxSortOrderByType(type: TransactionType): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countAll(): Int
}
