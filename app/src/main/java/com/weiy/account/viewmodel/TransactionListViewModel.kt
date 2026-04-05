package com.weiy.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.weiy.account.data.repository.SettingsRepository
import com.weiy.account.data.repository.TransactionRepository
import com.weiy.account.model.BillYearRange
import com.weiy.account.model.MonthBill
import com.weiy.account.model.YearBill
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class BillTab {
    MONTH,
    YEAR
}

data class BillSummaryCard(
    val title: String = "年结余",
    val incomeLabel: String = "年收入",
    val expenseLabel: String = "年支出",
    val incomeTotal: Double = 0.0,
    val expenseTotal: Double = 0.0
) {
    val balance: Double
        get() = incomeTotal - expenseTotal
}

data class TransactionListUiState(
    val tab: BillTab = BillTab.MONTH,
    val selectedYear: Int = 2002,
    val availableYears: List<Int> = listOf(2002),
    val minSelectableYear: Int = 2002,
    val maxSelectableYear: Int = 2002,
    val monthBills: List<MonthBill> = emptyList(),
    val yearBills: List<YearBill> = emptyList(),
    val summaryCard: BillSummaryCard = BillSummaryCard(),
    val showYearHint: Boolean = false
)

class TransactionListViewModel(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val initialYearRange = settingsRepository.currentBillYearRange
    private val selectedYear = MutableStateFlow(initialYearRange.maxYear)
    private val selectedTab = MutableStateFlow(BillTab.MONTH)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthBillsByYear = selectedYear.flatMapLatest { year ->
        transactionRepository.observeMonthBillsByYear(year)
    }
    private val yearBills = transactionRepository.observeYearBills()

    val uiState: StateFlow<TransactionListUiState> = combine(
        selectedTab,
        selectedYear,
        settingsRepository.billYearRange,
        monthBillsByYear,
        yearBills
    ) { tab, year, yearRange, monthRows, yearRows ->
        val safeYear = year.coerceIn(yearRange.minYear, yearRange.maxYear)

        val yearSummary = BillSummaryCard(
            title = "年结余",
            incomeLabel = "年收入",
            expenseLabel = "年支出",
            incomeTotal = monthRows.sumOf { it.incomeTotal },
            expenseTotal = monthRows.sumOf { it.expenseTotal }
        )
        val allYearSummary = BillSummaryCard(
            title = "总结余",
            incomeLabel = "总收入",
            expenseLabel = "总支出",
            incomeTotal = yearRows.sumOf { it.incomeTotal },
            expenseTotal = yearRows.sumOf { it.expenseTotal }
        )

        val availableYears = (yearRange.minYear..yearRange.maxYear).toList().sortedDescending()

        TransactionListUiState(
            tab = tab,
            selectedYear = safeYear,
            availableYears = availableYears,
            minSelectableYear = yearRange.minYear,
            maxSelectableYear = yearRange.maxYear,
            monthBills = monthRows,
            yearBills = yearRows,
            summaryCard = if (tab == BillTab.MONTH) yearSummary else allYearSummary,
            showYearHint = tab == BillTab.YEAR
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionListUiState(
            selectedYear = initialYearRange.maxYear,
            availableYears = (initialYearRange.minYear..initialYearRange.maxYear).toList().sortedDescending(),
            minSelectableYear = initialYearRange.minYear,
            maxSelectableYear = initialYearRange.maxYear
        )
    )

    fun refreshBillYearRange() {
        settingsRepository.refreshBillYearRange()
        val range: BillYearRange = settingsRepository.currentBillYearRange
        if (selectedYear.value !in range.minYear..range.maxYear) {
            selectedYear.value = range.maxYear
        }
    }

    fun selectTab(tab: BillTab) {
        selectedTab.value = tab
    }

    fun previousYear() {
        val range = settingsRepository.currentBillYearRange
        selectedYear.value = (selectedYear.value - 1).coerceAtLeast(range.minYear)
    }

    fun nextYear() {
        val range = settingsRepository.currentBillYearRange
        selectedYear.value = (selectedYear.value + 1).coerceAtMost(range.maxYear)
    }

    fun selectYear(year: Int) {
        val range = settingsRepository.currentBillYearRange
        selectedYear.value = year.coerceIn(range.minYear, range.maxYear)
    }

    companion object {
        fun factory(
            transactionRepository: TransactionRepository,
            settingsRepository: SettingsRepository
        ) = viewModelFactory {
            initializer {
                TransactionListViewModel(
                    transactionRepository = transactionRepository,
                    settingsRepository = settingsRepository
                )
            }
        }
    }
}
