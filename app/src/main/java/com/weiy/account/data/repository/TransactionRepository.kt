package com.weiy.account.data.repository

import com.weiy.account.data.local.dao.TransactionDao
import com.weiy.account.data.local.entity.MonthBillRaw
import com.weiy.account.data.local.entity.TransactionWithCategory
import com.weiy.account.data.local.entity.YearBillRaw
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
import kotlinx.coroutines.flow.map

class TransactionRepository(
    private val transactionDao: TransactionDao
) {

    fun observeRecentTransactions(limit: Int = 6): Flow<List<TransactionRecord>> {
        return transactionDao.observeRecentTransactions(limit).map { list -> list.map { it.toModel() } }
    }

    fun observeTransactionsByMonth(month: YearMonth): Flow<List<TransactionRecord>> {
        return transactionDao.observeTransactionsByMonth(
            monthStart = monthStartMillis(month),
            monthEnd = monthEndMillis(month)
        ).map { list -> list.map { it.toModel() } }
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

    suspend fun addTransaction(
        type: TransactionType,
        amount: Double,
        categoryId: Long,
        note: String,
        dateTime: Long
    ): Long {
        val now = System.currentTimeMillis()
        return transactionDao.insertTransaction(
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
    }

    suspend fun updateTransaction(
        id: Long,
        type: TransactionType,
        amount: Double,
        categoryId: Long,
        note: String,
        dateTime: Long
    ) {
        val existing = transactionDao.getTransactionById(id) ?: return
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
