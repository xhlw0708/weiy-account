package com.weiy.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.weiy.account.data.repository.TransactionRepository
import com.weiy.account.model.CategoryStat
import com.weiy.account.model.MonthSummary
import com.weiy.account.model.TransactionType
import com.weiy.account.utils.currentYearMonth
import com.weiy.account.utils.formatMonth
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val month: YearMonth = currentYearMonth(),
    val monthLabel: String = formatMonth(currentYearMonth()),
    val summary: MonthSummary = MonthSummary(),
    val expenseStats: List<CategoryStat> = emptyList(),
    val incomeStats: List<CategoryStat> = emptyList()
)

class StatsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(currentYearMonth())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = selectedMonth.flatMapLatest { month ->
        combine(
            transactionRepository.observeMonthSummary(month),
            transactionRepository.observeCategoryStatsByMonth(month, TransactionType.EXPENSE),
            transactionRepository.observeCategoryStatsByMonth(month, TransactionType.INCOME)
        ) { summary, expenseStats, incomeStats ->
            StatsUiState(
                month = month,
                monthLabel = formatMonth(month),
                summary = summary,
                expenseStats = expenseStats,
                incomeStats = incomeStats
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    companion object {
        fun factory(transactionRepository: TransactionRepository) = viewModelFactory {
            initializer {
                StatsViewModel(transactionRepository)
            }
        }
    }
}
