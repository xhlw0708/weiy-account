package com.weiy.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.weiy.account.data.repository.TransactionRepository
import com.weiy.account.model.CategoryStat
import com.weiy.account.model.MonthBill
import com.weiy.account.model.MonthSummary
import com.weiy.account.model.TransactionRecord
import com.weiy.account.model.TransactionType
import com.weiy.account.utils.currentYearMonth
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class MonthCategoryRatioItem(
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val ratio: Double,
    val type: TransactionType
)

data class MonthExpenseRankItem(
    val categoryName: String,
    val amount: Double,
    val dateTime: Long,
    val note: String
)

data class MonthCompareBarItem(
    val month: YearMonth,
    val amount: Double,
    val isCurrentMonth: Boolean
)

data class MonthCategoryChangeItem(
    val categoryId: Long,
    val categoryName: String,
    val delta: Double
) {
    val isIncrease: Boolean
        get() = delta >= 0
}

data class MonthBillDetailUiState(
    val yearMonth: YearMonth,
    val title: String,
    val currentSummary: MonthSummary = MonthSummary(),
    val previousSummary: MonthSummary = MonthSummary(),
    val expenseCategories: List<MonthCategoryRatioItem> = emptyList(),
    val incomeCategories: List<MonthCategoryRatioItem> = emptyList(),
    val expenseRanking: List<MonthExpenseRankItem> = emptyList(),
    val expenseRankingAll: List<MonthExpenseRankItem> = emptyList(),
    val expenseDailyTotals: List<Double> = List(yearMonth.lengthOfMonth()) { 0.0 },
    val maxExpenseDay: Int? = null,
    val maxExpenseDayAmount: Double = 0.0,
    val averageDailyExpense: Double = 0.0,
    val expenseCompareBars: List<MonthCompareBarItem> = emptyList(),
    val incomeCompareBars: List<MonthCompareBarItem> = emptyList(),
    val topChangedExpenseCategories: List<MonthCategoryChangeItem> = emptyList()
)

private data class SummaryBundle(
    val currentSummary: MonthSummary,
    val previousSummary: MonthSummary,
    val expenseCategories: List<CategoryStat>,
    val previousExpenseCategories: List<CategoryStat>
)

private data class DetailBundle(
    val incomeCategories: List<CategoryStat>,
    val transactions: List<TransactionRecord>,
    val currentYearMonthBills: List<MonthBill>,
    val compareYearMonthBills: List<MonthBill>
)

class MonthBillDetailViewModel(
    year: Int,
    month: Int,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val selectedMonth = YearMonth.of(year, month.coerceIn(1, 12))
    private val compareStartMonth = selectedMonth.minusMonths(COMPARE_MONTH_COUNT - 1)
    private val compareYear = compareStartMonth.year

    private val summaryBundleFlow: Flow<SummaryBundle> = combine(
        transactionRepository.observeMonthSummary(selectedMonth),
        transactionRepository.observeMonthSummary(selectedMonth.minusMonths(1)),
        transactionRepository.observeCategoryStatsByMonth(selectedMonth, TransactionType.EXPENSE),
        transactionRepository.observeCategoryStatsByMonth(selectedMonth.minusMonths(1), TransactionType.EXPENSE)
    ) { currentSummary, previousSummary, expenseCategories, previousExpenseCategories ->
        SummaryBundle(
            currentSummary = currentSummary,
            previousSummary = previousSummary,
            expenseCategories = expenseCategories,
            previousExpenseCategories = previousExpenseCategories
        )
    }

    private val detailBundleFlow: Flow<DetailBundle> = combine(
        transactionRepository.observeCategoryStatsByMonth(selectedMonth, TransactionType.INCOME),
        transactionRepository.observeTransactionsByMonth(selectedMonth),
        transactionRepository.observeMonthBillsByYear(selectedMonth.year),
        if (compareYear == selectedMonth.year) {
            flowOf(emptyList())
        } else {
            transactionRepository.observeMonthBillsByYear(compareYear)
        }
    ) { incomeCategories, transactions, currentYearMonthBills, compareYearMonthBills ->
        DetailBundle(
            incomeCategories = incomeCategories,
            transactions = transactions,
            currentYearMonthBills = currentYearMonthBills,
            compareYearMonthBills = compareYearMonthBills
        )
    }

    val uiState = combine(summaryBundleFlow, detailBundleFlow) { summary, detail ->
        val expenseCategories = summary.expenseCategories.toRatioItems()
        val incomeCategories = detail.incomeCategories.toRatioItems()
        val expenseTransactions = detail.transactions
            .asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .toList()
        val expenseRankingAll = expenseTransactions
            .sortedByDescending { it.amount }
            .map {
                MonthExpenseRankItem(
                    categoryName = it.categoryName,
                    amount = it.amount,
                    dateTime = it.dateTime,
                    note = it.note
                )
            }
        val expenseRanking = expenseRankingAll.take(TOP_RANKING_COUNT)

        val expenseDailyTotals = buildExpenseDailyTotals(expenseTransactions, selectedMonth)
        val maxExpensePair = expenseDailyTotals
            .withIndex()
            .maxByOrNull { it.value }
            ?.takeIf { it.value > 0.0 }
        val maxExpenseDay = maxExpensePair?.index?.plus(1)
        val maxExpenseDayAmount = maxExpensePair?.value ?: 0.0

        val averageDailyExpense = summary.currentSummary.expenseTotal / averageDailyDenominator(selectedMonth)

        val monthBillMap = buildMonthBillMap(
            selectedMonth = selectedMonth,
            currentYearMonthBills = detail.currentYearMonthBills,
            compareYearMonthBills = detail.compareYearMonthBills
        )
        val compareMonths = (0L until COMPARE_MONTH_COUNT).map { offset ->
            compareStartMonth.plusMonths(offset)
        }
        val expenseCompareBars = compareMonths.map { monthItem ->
            MonthCompareBarItem(
                month = monthItem,
                amount = monthBillMap[monthItem]?.expenseTotal ?: 0.0,
                isCurrentMonth = monthItem == selectedMonth
            )
        }
        val incomeCompareBars = compareMonths.map { monthItem ->
            MonthCompareBarItem(
                month = monthItem,
                amount = monthBillMap[monthItem]?.incomeTotal ?: 0.0,
                isCurrentMonth = monthItem == selectedMonth
            )
        }

        MonthBillDetailUiState(
            yearMonth = selectedMonth,
            title = "${selectedMonth.year}年${selectedMonth.monthValue}月账单",
            currentSummary = summary.currentSummary,
            previousSummary = summary.previousSummary,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            expenseRanking = expenseRanking,
            expenseRankingAll = expenseRankingAll,
            expenseDailyTotals = expenseDailyTotals,
            maxExpenseDay = maxExpenseDay,
            maxExpenseDayAmount = maxExpenseDayAmount,
            averageDailyExpense = averageDailyExpense,
            expenseCompareBars = expenseCompareBars,
            incomeCompareBars = incomeCompareBars,
            topChangedExpenseCategories = buildTopCategoryChanges(
                currentCategories = summary.expenseCategories,
                previousCategories = summary.previousExpenseCategories
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MonthBillDetailUiState(
            yearMonth = selectedMonth,
            title = "${selectedMonth.year}年${selectedMonth.monthValue}月账单"
        )
    )

    private fun averageDailyDenominator(month: YearMonth): Double {
        val days = month.lengthOfMonth()
        if (month == currentYearMonth()) {
            val currentDay = LocalDate.now().dayOfMonth.coerceIn(1, days)
            return currentDay.toDouble()
        }
        return days.toDouble()
    }

    private fun buildExpenseDailyTotals(
        expenseTransactions: List<TransactionRecord>,
        month: YearMonth
    ): List<Double> {
        val totals = MutableList(month.lengthOfMonth()) { 0.0 }
        expenseTransactions.forEach { transaction ->
            val dayIndex = transaction.toLocalDayOfMonth() - 1
            if (dayIndex in totals.indices) {
                totals[dayIndex] += transaction.amount
            }
        }
        return totals
    }

    private fun buildMonthBillMap(
        selectedMonth: YearMonth,
        currentYearMonthBills: List<MonthBill>,
        compareYearMonthBills: List<MonthBill>
    ): Map<YearMonth, MonthBill> {
        val result = mutableMapOf<YearMonth, MonthBill>()
        currentYearMonthBills.forEach { bill ->
            result[YearMonth.of(selectedMonth.year, bill.month)] = bill
        }
        compareYearMonthBills.forEach { bill ->
            result[YearMonth.of(compareYear, bill.month)] = bill
        }
        return result
    }

    private fun buildTopCategoryChanges(
        currentCategories: List<CategoryStat>,
        previousCategories: List<CategoryStat>
    ): List<MonthCategoryChangeItem> {
        val currentMap = currentCategories.associateBy { it.categoryId }
        val previousMap = previousCategories.associateBy { it.categoryId }

        return (currentMap.keys + previousMap.keys)
            .asSequence()
            .mapNotNull { categoryId ->
                val current = currentMap[categoryId]
                val previous = previousMap[categoryId]
                val delta = (current?.totalAmount ?: 0.0) - (previous?.totalAmount ?: 0.0)
                if (abs(delta) < CHANGE_EPSILON) {
                    null
                } else {
                    MonthCategoryChangeItem(
                        categoryId = categoryId,
                        categoryName = current?.categoryName ?: previous?.categoryName.orEmpty(),
                        delta = delta
                    )
                }
            }
            .sortedByDescending { abs(it.delta) }
            .take(TOP_RANKING_COUNT)
            .toList()
    }

    private fun List<CategoryStat>.toRatioItems(): List<MonthCategoryRatioItem> {
        val effectiveItems = this.filter { it.totalAmount > 0.0 }
        val total = effectiveItems.sumOf { it.totalAmount }
        if (total <= 0.0) {
            return emptyList()
        }

        return effectiveItems.map { item ->
            MonthCategoryRatioItem(
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                amount = item.totalAmount,
                ratio = item.totalAmount / total,
                type = item.type
            )
        }
    }

    private fun TransactionRecord.toLocalDayOfMonth(): Int {
        val localDate = java.time.Instant.ofEpochMilli(dateTime)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        return localDate.dayOfMonth
    }

    companion object {
        private const val TOP_RANKING_COUNT = 3
        private const val COMPARE_MONTH_COUNT = 6L
        private const val CHANGE_EPSILON = 0.005

        fun factory(
            year: Int,
            month: Int,
            transactionRepository: TransactionRepository
        ) = viewModelFactory {
            initializer {
                MonthBillDetailViewModel(
                    year = year,
                    month = month,
                    transactionRepository = transactionRepository
                )
            }
        }
    }
}
