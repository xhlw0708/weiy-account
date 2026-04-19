package com.weiy.account.data.repository

import androidx.room.withTransaction
import com.weiy.account.data.local.database.AppDatabase
import com.weiy.account.data.local.entity.CategoryNoteHistoryEntity
import com.weiy.account.data.local.entity.MonthBillRaw
import com.weiy.account.data.local.entity.TransactionWithCategory
import com.weiy.account.data.local.entity.YearBillRaw
import com.weiy.account.model.CategoryNoteHistoryItem
import com.weiy.account.model.CategoryStat
import com.weiy.account.model.MonthBill
import com.weiy.account.model.MonthSummary
import com.weiy.account.model.TransactionRecord
import com.weiy.account.model.TransactionType
import com.weiy.account.model.YearBill
import com.weiy.account.utils.monthEndMillis
import com.weiy.account.utils.monthStartMillis
import com.weiy.account.utils.yearEndMillis
import com.weiy.account.utils.yearStartMillis
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class TransactionRepository(
    private val database: AppDatabase
) {
    private val transactionDao = database.transactionDao()
    private val categoryNoteHistoryDao = database.categoryNoteHistoryDao()

    fun observeRecentTransactions(limit: Int = 6): Flow<List<TransactionRecord>> {
        return transactionDao.observeRecentTransactions(limit).map { list -> list.map { it.toModel() } }
    }

    fun observeTransactionsByMonth(month: YearMonth): Flow<List<TransactionRecord>> {
        return transactionDao.observeTransactionsByMonth(
            monthStart = monthStartMillis(month),
            monthEnd = monthEndMillis(month)
        ).map { list -> list.map { it.toModel() } }
    }

    fun observeTransactionsByKeyword(keyword: String): Flow<List<TransactionRecord>> {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) {
            return flowOf(emptyList())
        }
        return transactionDao.observeTransactionsByKeyword(normalizedKeyword)
            .map { list -> list.map { it.toModel() } }
    }

    fun observeMonthSummary(month: YearMonth): Flow<MonthSummary> {
        return transactionDao.observeMonthlySummary(
            monthStart = monthStartMillis(month),
            monthEnd = monthEndMillis(month)
        ).map { raw ->
            MonthSummary(
                incomeTotal = raw.incomeTotal,
                expenseTotal = raw.expenseTotal
            )
        }
    }

    fun observeCategoryStatsByMonth(
        month: YearMonth,
        type: TransactionType
    ): Flow<List<CategoryStat>> {
        return transactionDao.observeCategoryStatsByMonth(
            type = type,
            monthStart = monthStartMillis(month),
            monthEnd = monthEndMillis(month)
        ).map { list ->
            list.map { raw ->
                CategoryStat(
                    categoryId = raw.categoryId,
                    categoryName = raw.categoryName,
                    type = raw.type,
                    totalAmount = raw.totalAmount
                )
            }
        }
    }

    fun observeMonthBillsByYear(year: Int): Flow<List<MonthBill>> {
        return transactionDao.observeMonthBillsByYear(
            yearStart = yearStartMillis(year),
            yearEnd = yearEndMillis(year)
        ).map { list -> list.map { it.toModel() } }
    }

    fun observeYearBills(): Flow<List<YearBill>> {
        return transactionDao.observeYearBills().map { list -> list.map { it.toModel() } }
    }

    suspend fun getTransactionById(id: Long): TransactionRecord? {
        return transactionDao.getTransactionWithCategoryById(id)?.toModel()
    }

    fun observeCategoryNoteHistories(categoryId: Long): Flow<List<CategoryNoteHistoryItem>> {
        return categoryNoteHistoryDao.observeHistoriesByCategoryId(categoryId).map { histories ->
            histories.map {
                CategoryNoteHistoryItem(
                    note = it.note,
                    usageCount = it.usageCount
                )
            }
        }
    }

    suspend fun deleteCategoryNoteHistory(
        categoryId: Long,
        note: String
    ) {
        val normalizedNote = note.trim()
        if (normalizedNote.isBlank()) return
        categoryNoteHistoryDao.deleteHistoryByCategoryIdAndNote(
            categoryId = categoryId,
            note = normalizedNote
        )
    }

    suspend fun addTransaction(
        type: TransactionType,
        amount: Double,
        categoryId: Long,
        note: String,
        dateTime: Long
    ): Long {
        val now = System.currentTimeMillis()
        return database.withTransaction {
            val transactionId = transactionDao.insertTransaction(
                com.weiy.account.data.local.entity.TransactionEntity(
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    note = note,
                    dateTime = dateTime,
                    createdAt = now,
                    updatedAt = now
                )
            )
            recordCategoryNoteHistory(
                categoryId = categoryId,
                note = note,
                usedAt = dateTime
            )
            transactionId
        }
    }

    suspend fun updateTransaction(
        id: Long,
        type: TransactionType,
        amount: Double,
        categoryId: Long,
        note: String,
        dateTime: Long
    ) {
        database.withTransaction {
            val existing = transactionDao.getTransactionById(id) ?: return@withTransaction
            transactionDao.updateTransaction(
                existing.copy(
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    note = note,
                    dateTime = dateTime,
                    updatedAt = System.currentTimeMillis()
                )
            )
            val previousNote = existing.note.trim()
            val currentNote = note.trim()
            if (currentNote.isNotBlank() &&
                (existing.categoryId != categoryId || previousNote != currentNote)
            ) {
                recordCategoryNoteHistory(
                    categoryId = categoryId,
                    note = currentNote,
                    usedAt = dateTime
                )
            }
        }
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun deleteTransactionsByMonth(month: YearMonth) {
        transactionDao.deleteByDateRange(
            start = monthStartMillis(month),
            end = monthEndMillis(month)
        )
    }

    private suspend fun recordCategoryNoteHistory(
        categoryId: Long,
        note: String,
        usedAt: Long
    ) {
        val normalizedNote = note.trim()
        if (normalizedNote.isBlank()) return

        val now = System.currentTimeMillis()
        val existing = categoryNoteHistoryDao.getHistoryByCategoryIdAndNote(
            categoryId = categoryId,
            note = normalizedNote
        )
        if (existing == null) {
            categoryNoteHistoryDao.insertHistory(
                CategoryNoteHistoryEntity(
                    categoryId = categoryId,
                    note = normalizedNote,
                    usageCount = 1,
                    lastUsedAt = usedAt,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            categoryNoteHistoryDao.updateHistory(
                existing.copy(
                    usageCount = existing.usageCount + 1,
                    lastUsedAt = maxOf(existing.lastUsedAt, usedAt),
                    updatedAt = now
                )
            )
        }
    }
}

private fun TransactionWithCategory.toModel(): TransactionRecord {
    return TransactionRecord(
        id = transaction.id,
        type = transaction.type,
        amount = transaction.amount,
        categoryId = transaction.categoryId,
        categoryName = categoryName,
        note = transaction.note,
        dateTime = transaction.dateTime,
        createdAt = transaction.createdAt,
        updatedAt = transaction.updatedAt
    )
}

private fun MonthBillRaw.toModel(): MonthBill {
    return MonthBill(
        month = month,
        incomeTotal = incomeTotal,
        expenseTotal = expenseTotal
    )
}

private fun YearBillRaw.toModel(): YearBill {
    return YearBill(
        year = year,
        incomeTotal = incomeTotal,
        expenseTotal = expenseTotal
    )
}
