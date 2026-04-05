package com.weiy.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.weiy.account.data.repository.CategoryRepository
import com.weiy.account.data.repository.SettingsRepository
import com.weiy.account.data.repository.TransactionRepository
import com.weiy.account.model.CategoryItem
import com.weiy.account.model.TransactionType
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

data class TransactionEditUiState(
    val transactionId: Long = 0L,
    val isEditMode: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val categoryId: Long? = null,
    val categories: List<CategoryItem> = emptyList(),
    val note: String = "",
    val dateTime: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    val canDelete: Boolean
        get() = isEditMode
}

sealed interface TransactionEditEvent {
    data object Saved : TransactionEditEvent
    data object Deleted : TransactionEditEvent
}

private data class EditCoreState(
    val isEditMode: Boolean,
    val type: TransactionType,
    val amountInput: String,
    val categoryId: Long?
)

private data class EditAuxState(
    val categories: List<CategoryItem>,
    val note: String,
    val dateTime: Long,
    val errorMessage: String?
)

class TransactionEditViewModel(
    private val transactionIdArg: Long,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val isEditMode = MutableStateFlow(transactionIdArg > 0L)
    private val type = MutableStateFlow(settingsRepository.currentSettings.defaultTransactionType)
    private val amountInput = MutableStateFlow("")
    private val categoryId = MutableStateFlow<Long?>(null)
    private val note = MutableStateFlow("")
    private val dateTime = MutableStateFlow(System.currentTimeMillis())
    private val errorMessage = MutableStateFlow<String?>(null)

    private val events = MutableSharedFlow<TransactionEditEvent>()
    val uiEvents = events.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val categories = type.flatMapLatest { categoryRepository.observeCategoriesByType(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val coreState = combine(
        isEditMode,
        type,
        amountInput,
        categoryId
    ) { isEditModeValue, typeValue, amountValue, categoryIdValue ->
        EditCoreState(
            isEditMode = isEditModeValue,
            type = typeValue,
            amountInput = amountValue,
            categoryId = categoryIdValue
        )
    }

    private val auxState = combine(
        categories,
        note,
        dateTime,
        errorMessage
    ) { categoriesValue, noteValue, dateTimeValue, errorValue ->
        EditAuxState(
            categories = categoriesValue,
            note = noteValue,
            dateTime = dateTimeValue,
            errorMessage = errorValue
        )
    }

    val uiState: StateFlow<TransactionEditUiState> = combine(coreState, auxState) { core, aux ->
        TransactionEditUiState(
            transactionId = transactionIdArg,
            isEditMode = core.isEditMode,
            type = core.type,
            amountInput = core.amountInput,
            categoryId = core.categoryId,
            categories = aux.categories,
            note = aux.note,
            dateTime = aux.dateTime,
            errorMessage = aux.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionEditUiState()
    )

    init {
        viewModelScope.launch {
            categories.collect { list ->
                if (list.isNotEmpty()) {
                    val currentId = categoryId.value
                    if (currentId == null || list.none { it.id == currentId }) {
                        categoryId.value = list.first().id
                    }
                }
            }
        }

        if (transactionIdArg > 0L) {
            loadTransaction(transactionIdArg)
        }
    }

    private fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId) ?: return@launch
            isEditMode.value = true
            type.value = transaction.type
            amountInput.value = transaction.amount.toString()
            categoryId.value = transaction.categoryId
            note.value = transaction.note
            dateTime.value = transaction.dateTime
        }
    }

    fun onTypeChange(newType: TransactionType) {
        type.value = newType
    }

    fun onAmountChange(value: String) {
        amountInput.value = value
    }

    fun onCategoryChange(newCategoryId: Long) {
        categoryId.value = newCategoryId
    }

    fun onNoteChange(value: String) {
        note.value = value
    }

    fun onDateTimeChange(value: Long) {
        dateTime.value = value
    }

    fun saveTransaction() {
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
        errorMessage.value = null

        viewModelScope.launch {
            if (transactionIdArg > 0L) {
                transactionRepository.updateTransaction(
                    id = transactionIdArg,
                    type = type.value,
                    amount = amount,
                    categoryId = selectedCategoryId,
                    note = note.value.trim(),
                    dateTime = dateTime.value
                )
            } else {
                transactionRepository.addTransaction(
                    type = type.value,
                    amount = amount,
                    categoryId = selectedCategoryId,
                    note = note.value.trim(),
                    dateTime = dateTime.value
                )
            }
            events.emit(TransactionEditEvent.Saved)
        }
    }

    fun deleteTransaction() {
        if (transactionIdArg <= 0L) return
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionIdArg)
            events.emit(TransactionEditEvent.Deleted)
        }
    }

    companion object {
        fun factory(
            transactionId: Long,
            transactionRepository: TransactionRepository,
            categoryRepository: CategoryRepository,
            settingsRepository: SettingsRepository
        ) = viewModelFactory {
            initializer {
                TransactionEditViewModel(
                    transactionIdArg = transactionId,
                    transactionRepository = transactionRepository,
                    categoryRepository = categoryRepository,
                    settingsRepository = settingsRepository
                )
            }
        }
    }
}
