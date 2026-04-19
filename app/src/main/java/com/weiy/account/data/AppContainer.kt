package com.weiy.account.data

import android.content.Context
import com.weiy.account.BuildConfig
import com.weiy.account.data.local.database.AppDatabase
import com.weiy.account.data.repository.CategoryRepository
import com.weiy.account.data.repository.DataTransferRepository
import com.weiy.account.data.repository.RecurringAccountingRepository
import com.weiy.account.data.repository.SearchHistoryRepository
import com.weiy.account.data.repository.SettingsRepository
import com.weiy.account.data.repository.TransactionRepository
import com.weiy.account.model.TransactionType
import com.weiy.account.preferences.SettingsPreferencesDataSource
import com.weiy.account.utils.toMillis
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: AppDatabase = AppDatabase.getInstance(context)
    private val settingsDataSource = SettingsPreferencesDataSource(context)

    val transactionRepository: TransactionRepository = TransactionRepository(database)
    val categoryRepository: CategoryRepository = CategoryRepository(database.categoryDao())
    val settingsRepository: SettingsRepository = SettingsRepository(settingsDataSource)
    val searchHistoryRepository: SearchHistoryRepository = SearchHistoryRepository(context)
    val dataTransferRepository: DataTransferRepository = DataTransferRepository(context, database)
    val recurringAccountingRepository: RecurringAccountingRepository = RecurringAccountingRepository(
        database = database,
        transactionRepository = transactionRepository
    )

    init {
        appScope.launch {
            categoryRepository.ensureDefaultCategories()
            if (BuildConfig.ENABLE_DEMO_SEED) {
                seedDemoTransactionsIfNeeded()
            }
        }
    }

    private suspend fun seedDemoTransactionsIfNeeded() {
        if (settingsDataSource.isDemoData2026FebMarSeeded()) return

        val expenseCategoryId = categoryRepository.getFirstCategoryIdByType(TransactionType.EXPENSE) ?: return
        val incomeCategoryId = categoryRepository.getFirstCategoryIdByType(TransactionType.INCOME) ?: return

        seedMonthData(
            month = YearMonth.of(2026, 2),
            incomeAmount = 9000.0,
            incomeCategoryId = incomeCategoryId,
            expenseCategoryId = expenseCategoryId
        )
        seedMonthData(
            month = YearMonth.of(2026, 3),
            incomeAmount = 11203.73,
            incomeCategoryId = incomeCategoryId,
            expenseCategoryId = expenseCategoryId
        )

        settingsDataSource.setDemoData2026FebMarSeeded(true)
    }

    private suspend fun seedMonthData(
        month: YearMonth,
        incomeAmount: Double,
        incomeCategoryId: Long,
        expenseCategoryId: Long
    ) {
        transactionRepository.deleteTransactionsByMonth(month)

        val incomeDateTime = month.atDay(1).atTime(9, 0)
        transactionRepository.addTransaction(
            type = TransactionType.INCOME,
            amount = incomeAmount,
            categoryId = incomeCategoryId,
            note = "工资",
            dateTime = toMillis(incomeDateTime)
        )

        val days = month.lengthOfMonth()
        for (day in 1..days) {
            val breakfastTime = LocalDateTime.of(month.year, month.month, day, 7, 50)
            val lunchTime = LocalDateTime.of(month.year, month.month, day, 12, 20)
            val dinnerTime = LocalDateTime.of(month.year, month.month, day, 18, 40)

            transactionRepository.addTransaction(
                type = TransactionType.EXPENSE,
                amount = 12.0,
                categoryId = expenseCategoryId,
                note = "早餐",
                dateTime = toMillis(breakfastTime)
            )
            transactionRepository.addTransaction(
                type = TransactionType.EXPENSE,
                amount = 18.0,
                categoryId = expenseCategoryId,
                note = "午饭",
                dateTime = toMillis(lunchTime)
            )
            transactionRepository.addTransaction(
                type = TransactionType.EXPENSE,
                amount = 20.0,
                categoryId = expenseCategoryId,
                note = "晚饭",
                dateTime = toMillis(dinnerTime)
            )
        }
    }
}
