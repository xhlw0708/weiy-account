package com.weiy.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.weiy.account.data.repository.SearchHistoryRepository
import com.weiy.account.data.repository.TransactionRepository
import com.weiy.account.model.TransactionDateGroup
import com.weiy.account.utils.formatMonth
import com.weiy.account.utils.toLocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val keywordInput: String = "",
    val submittedKeyword: String = "",
    val histories: List<String> = emptyList(),
    val groupedResults: List<TransactionDateGroup> = emptyList()
)

class SearchViewModel(
    private val transactionRepository: TransactionRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    private val keywordInput = MutableStateFlow("")
    private val submittedKeyword = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchResultFlow = submittedKeyword.flatMapLatest { keyword ->
        transactionRepository.observeTransactionsByKeyword(keyword)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        keywordInput,
        submittedKeyword,
        searchHistoryRepository.histories,
        searchResultFlow
    ) { currentInput, currentSubmittedKeyword, histories, searchResults ->
        val grouped = searchResults
            .groupBy { record ->
                val month = YearMonth.from(toLocalDateTime(record.dateTime))
                formatMonth(month)
            }
            .map { (monthLabel, items) ->
                TransactionDateGroup(
                    dateLabel = monthLabel,
                    items = items
                )
            }

        SearchUiState(
            keywordInput = currentInput,
            submittedKeyword = currentSubmittedKeyword,
            histories = histories,
            groupedResults = grouped
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState()
    )

    fun onKeywordChange(value: String) {
        keywordInput.value = value
        if (value.isBlank()) {
            submittedKeyword.value = ""
        }
    }

    fun search() {
        val normalizedKeyword = keywordInput.value.trim()
        if (normalizedKeyword.isBlank()) {
            submittedKeyword.value = ""
            return
        }

        submittedKeyword.value = normalizedKeyword
        searchHistoryRepository.record(normalizedKeyword)
    }

    fun searchByHistory(keyword: String) {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) return

        keywordInput.value = normalizedKeyword
        submittedKeyword.value = normalizedKeyword
        searchHistoryRepository.record(normalizedKeyword)
    }

    fun removeHistory(keyword: String) {
        searchHistoryRepository.remove(keyword)
    }

    fun clearHistories() {
        searchHistoryRepository.clear()
    }

    companion object {
        fun factory(
            transactionRepository: TransactionRepository,
            searchHistoryRepository: SearchHistoryRepository
        ) = viewModelFactory {
            initializer {
                SearchViewModel(
                    transactionRepository = transactionRepository,
                    searchHistoryRepository = searchHistoryRepository
                )
            }
        }
    }
}

