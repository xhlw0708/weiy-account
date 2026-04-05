package com.weiy.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.weiy.account.data.repository.CategoryRepository
import com.weiy.account.model.CategoryItem
import com.weiy.account.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryManageUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val categories: List<CategoryItem> = emptyList()
)

class CategoryManageViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val selectedType = MutableStateFlow(TransactionType.EXPENSE)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CategoryManageUiState> = selectedType
        .flatMapLatest { type ->
            categoryRepository.observeCategoriesByType(type).map { categories ->
                CategoryManageUiState(
                    selectedType = type,
                    categories = categories
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryManageUiState()
        )

    fun selectType(type: TransactionType) {
        selectedType.value = type
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.addCategory(name = name, type = selectedType.value)
        }
    }

    fun renameCategory(id: Long, newName: String) {
        viewModelScope.launch {
            categoryRepository.renameCategory(id = id, newName = newName)
        }
    }

    companion object {
        fun factory(categoryRepository: CategoryRepository) = viewModelFactory {
            initializer {
                CategoryManageViewModel(categoryRepository)
            }
        }
    }
}
