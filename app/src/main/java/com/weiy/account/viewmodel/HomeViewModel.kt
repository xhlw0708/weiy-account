package com.weiy.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.weiy.account.data.repository.TransactionRepository
import com.weiy.account.model.MonthSummary
import com.weiy.account.model.TransactionDateGroup
import com.weiy.account.utils.currentYearMonth
import com.weiy.account.utils.formatDateHeader
import com.weiy.account.utils.formatMonth
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentMonth: YearMonth = currentYearMonth(),
    val monthLabel: String = formatMonth(currentYearMonth()),
    val summary: MonthSummary = MonthSummary(),
    val groupedTransactions: List<TransactionDateGroup> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingPreviousMonth: Boolean = false
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val latestMonth = currentYearMonth()
    private val selectedMonth = MutableStateFlow(latestMonth)
    private val refreshing = MutableStateFlow(false)
    private val loadingPreviousMonth = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthSummaryFlow = selectedMonth.flatMapLatest { month ->
        transactionRepository.observeMonthSummary(month)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val groupedTransactionsFlow = selectedMonth.flatMapLatest { month ->
        transactionRepository.observeTransactionsByMonth(month)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        selectedMonth,
        monthSummaryFlow,
        groupedTransactionsFlow,
        refreshing,
        loadingPreviousMonth
    ) { month, summary, transactions, isRefreshing, isLoadingPreviousMonth ->
        val grouped = transactions
            .groupBy { formatDateHeader(it.dateTime) }
            .map { (dateLabel, items) ->
                TransactionDateGroup(
                    dateLabel = dateLabel,
                    items = items
                )
            }

        HomeUiState(
            currentMonth = month,
            monthLabel = formatMonth(month),
            summary = summary,
            groupedTransactions = grouped,
            isRefreshing = isRefreshing,
            isLoadingPreviousMonth = isLoadingPreviousMonth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun refreshOrLoadNextMonth() {
        if (refreshing.value || loadingPreviousMonth.value) return

        viewModelScope.launch {
            refreshing.value = true
            if (selectedMonth.value < latestMonth) {
                selectedMonth.value = selectedMonth.value.plusMonths(1)
            } else {
                // 最新月份下拉仅执行一次刷新反馈，不切换月份。
                delay(350)
            }
            refreshing.value = false
        }
    }

    fun loadPreviousMonthByPullUp() {
        if (refreshing.value || loadingPreviousMonth.value) return

        viewModelScope.launch {
            loadingPreviousMonth.value = true
            selectedMonth.value = selectedMonth.value.minusMonths(1)
            loadingPreviousMonth.value = false
        }
    }

    companion object {
        fun factory(transactionRepository: TransactionRepository) = viewModelFactory {
            initializer {
                HomeViewModel(transactionRepository)
            }
        }
    }
}
