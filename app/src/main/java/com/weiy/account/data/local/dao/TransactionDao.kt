package com.weiy.account.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.weiy.account.data.local.entity.CategoryStatRaw
import com.weiy.account.data.local.entity.MonthBillRaw
import com.weiy.account.data.local.entity.MonthlySummaryRaw
import com.weiy.account.data.local.entity.TransactionEntity
import com.weiy.account.data.local.entity.TransactionWithCategory
import com.weiy.account.data.local.entity.YearBillRaw
import com.weiy.account.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(entity: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(entity: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE dateTime BETWEEN :start AND :end")
    suspend fun deleteByDateRange(start: Long, end: Long)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query(
        """
        SELECT t.*, c.name AS category_name, c.type AS category_type
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.id = :id
        LIMIT 1
        """
    )
    suspend fun getTransactionWithCategoryById(id: Long): TransactionWithCategory?

    @Query(
        """
        SELECT t.*, c.name AS category_name, c.type AS category_type
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        ORDER BY t.dateTime DESC
        LIMIT :limit
        """
    )
    fun observeRecentTransactions(limit: Int): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT *
        FROM transactions
        ORDER BY dateTime ASC, id ASC
        """
    )
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query(
        """
        SELECT t.*, c.name AS category_name, c.type AS category_type
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.dateTime BETWEEN :monthStart AND :monthEnd
        ORDER BY t.dateTime DESC
        """
    )
    fun observeTransactionsByMonth(monthStart: Long, monthEnd: Long): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT t.*, c.name AS category_name, c.type AS category_type
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE c.name LIKE '%' || :keyword || '%'
            OR t.note LIKE '%' || :keyword || '%'
        ORDER BY t.dateTime DESC
        """
    )
    fun observeTransactionsByKeyword(keyword: String): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income_total,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense_total
        FROM transactions
        WHERE dateTime BETWEEN :monthStart AND :monthEnd
        """
    )
    fun observeMonthlySummary(monthStart: Long, monthEnd: Long): Flow<MonthlySummaryRaw>

    @Query(
        """
        SELECT
            c.id AS category_id,
            c.name AS category_name,
            c.type AS type,
            COALESCE(SUM(t.amount), 0) AS total_amount
        FROM categories c
        LEFT JOIN transactions t
            ON c.id = t.categoryId
            AND t.type = :type
            AND t.dateTime BETWEEN :monthStart AND :monthEnd
        WHERE c.type = :type
        GROUP BY c.id, c.name, c.type, c.sortOrder
        ORDER BY total_amount DESC, c.sortOrder ASC, c.id ASC
        """
    )
    fun observeCategoryStatsByMonth(
        type: TransactionType,
        monthStart: Long,
        monthEnd: Long
    ): Flow<List<CategoryStatRaw>>

    @Query(
        """
        SELECT
            CAST(strftime('%m', datetime(dateTime / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS month,
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income_total,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense_total
        FROM transactions
        WHERE dateTime BETWEEN :yearStart AND :yearEnd
        GROUP BY month
        ORDER BY month DESC
        """
    )
    fun observeMonthBillsByYear(yearStart: Long, yearEnd: Long): Flow<List<MonthBillRaw>>

    @Query(
        """
        SELECT
            CAST(strftime('%Y', datetime(dateTime / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS year,
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income_total,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense_total
        FROM transactions
        GROUP BY year
        ORDER BY year DESC
        """
    )
    fun observeYearBills(): Flow<List<YearBillRaw>>
}
