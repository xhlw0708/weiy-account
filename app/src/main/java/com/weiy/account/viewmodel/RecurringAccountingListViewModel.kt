package com.weiy.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.weiy.account.data.repository.RecurringAccountingRepository
import com.weiy.account.model.RecurringAccountingRuleItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringAccountingListUiState(
    val rules: List<RecurringAccountingRuleItem> = emptyList()
)

class RecurringAccountingListViewModel(
    private val repository: RecurringAccountingRepository
) : ViewModel() {

    val uiState: StateFlow<RecurringAccountingListUiState> = repository.observeRules()
        .map { RecurringAccountingListUiState(rules = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecurringAccountingListUiState()
        )

    fun setRuleEnabled(
        ruleId: Long,
        enabled: Boolean
    ) {
        viewModelScope.launch {
            repository.updateRuleEnabled(ruleId, enabled)
        }
    }

    companion object {
        fun factory(repository: RecurringAccountingRepository) = viewModelFactory {
            initializer {
                RecurringAccountingListViewModel(repository = repository)
            }
        }
    }
}
