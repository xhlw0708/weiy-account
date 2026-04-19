package com.weiy.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.weiy.account.data.repository.CategoryRepository
import com.weiy.account.data.repository.RecurringAccountingRepository
import com.weiy.account.model.CategoryItem
import com.weiy.account.model.RecurrenceUnit
import com.weiy.account.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringAccountingCreateUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val categoryId: Long? = null,
    val categories: List<CategoryItem> = emptyList(),
    val firstDateEpochMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val repeatUnit: RecurrenceUnit = RecurrenceUnit.MONTHLY,
    val errorMessage: String? = null
)

sealed interface RecurringAccountingCreateEvent {
    data object Saved : RecurringAccountingCreateEvent
}

private data class RecurringCreateCoreState(
    val type: TransactionType,
    val amountInput: String,
    val categoryId: Long?
)

private data class RecurringCreateAuxState(
    val categories: List<CategoryItem>,
    val firstDateEpochMillis: Long,
    val note: String,
    val repeatUnit: RecurrenceUnit,
    val errorMessage: String?
)

class RecurringAccountingCreateViewModel(
    private val recurringRepository: RecurringAccountingRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val type = MutableStateFlow(TransactionType.EXPENSE)
    private val amountInput = MutableStateFlow("")
    private val categoryId = MutableStateFlow<Long?>(null)
    private val firstDateEpochMillis = MutableStateFlow(System.currentTimeMillis())
    private val note = MutableStateFlow("")
    private val repeatUnit = MutableStateFlow(RecurrenceUnit.MONTHLY)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val events = MutableSharedFlow<RecurringAccountingCreateEvent>()
    val uiEvents = events.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val categories = type.flatMapLatest { selectedType ->
        categoryRepository.observeCategoriesByType(selectedType)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val coreState = combine(type, amountInput, categoryId) { currentType, currentAmount, currentCategoryId ->
        RecurringCreateCoreState(
            type = currentType,
            amountInput = currentAmount,
            categoryId = currentCategoryId
        )
    }

    private val auxWithoutError = combine(
        categories,
        firstDateEpochMillis,
        note,
        repeatUnit
    ) { currentCategories, currentFirstDate, currentNote, currentRepeat ->
        RecurringCreateAuxState(
            categories = currentCategories,
            firstDateEpochMillis = currentFirstDate,
            note = currentNote,
            repeatUnit = currentRepeat,
            errorMessage = null
        )
    }

    private val auxState = combine(auxWithoutError, errorMessage) { aux, currentError ->
        aux.copy(errorMessage = currentError)
    }

    val uiState: StateFlow<RecurringAccountingCreateUiState> = combine(coreState, auxState) { core, aux ->
        val fallbackCategoryId = core.categoryId?.takeIf { id ->
            aux.categories.any { it.id == id }
        } ?: aux.categories.firstOrNull()?.id
        if (fallbackCategoryId != core.categoryId) {
            categoryId.value = fallbackCategoryId
        }

        RecurringAccountingCreateUiState(
            type = core.type,
            amountInput = core.amountInput,
            categoryId = fallbackCategoryId,
            categories = aux.categories,
            firstDateEpochMillis = aux.firstDateEpochMillis,
            note = aux.note,
            repeatUnit = aux.repeatUnit,
            errorMessage = aux.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecurringAccountingCreateUiState()
    )

    fun onTypeChange(newType: TransactionType) {
        type.value = newType
        errorMessage.value = null
    }

    fun onAmountChange(value: String) {
        amountInput.value = value
    }

    fun onCategoryChange(newCategoryId: Long) {
        categoryId.value = newCategoryId
    }

    fun onFirstDateChange(value: Long) {
        firstDateEpochMillis.value = value
    }

    fun onNoteChange(value: String) {
        note.value = value
    }

    fun onRepeatUnitChange(value: RecurrenceUnit) {
        repeatUnit.value = value
    }

    fun save() {
        val amount = amountInput.value.trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            errorMessage.value = "请输入有效金额"
            return
        }
        val selectedCategoryId = categoryId.value
        if (selectedCategoryId == null) {
            errorMessage.value = "请选择分类"
            return
        }

        val firstDateEpochDay = Instant.ofEpochMilli(firstDateEpochMillis.value)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()

        errorMessage.value = null
        viewModelScope.launch {
            recurringRepository.addRule(
                type = type.value,
                amount = amount,
                categoryId = selectedCategoryId,
                note = note.value,
                firstOccurrenceDateEpochDay = firstDateEpochDay,
                repeatUnit = repeatUnit.value,
                enabled = true
            )
            events.emit(RecurringAccountingCreateEvent.Saved)
        }
    }

    companion object {
        fun factory(
            recurringRepository: RecurringAccountingRepository,
            categoryRepository: CategoryRepository
        ) = viewModelFactory {
            initializer {
                RecurringAccountingCreateViewModel(
                    recurringRepository = recurringRepository,
                    categoryRepository = categoryRepository
                )
            }
        }
    }
}
