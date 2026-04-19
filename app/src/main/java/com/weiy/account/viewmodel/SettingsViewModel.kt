package com.weiy.account.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.weiy.account.data.repository.DataTransferRepository
import com.weiy.account.data.repository.SettingsRepository
import com.weiy.account.model.DataTransferFormat
import com.weiy.account.model.StartDestination
import com.weiy.account.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val dataTransferRepository: DataTransferRepository
) : ViewModel() {

    val uiState: StateFlow<com.weiy.account.model.AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = settingsRepository.currentSettings
    )
    private val _dataTransferState = MutableStateFlow(DataTransferUiState())
    val dataTransferState: StateFlow<DataTransferUiState> = _dataTransferState.asStateFlow()

    fun setOnboardingShown(shown: Boolean) {
        settingsRepository.setOnboardingShown(shown)
    }

    fun setDefaultTransactionType(type: TransactionType) {
        settingsRepository.setDefaultTransactionType(type)
    }

    fun setDefaultStartDestination(destination: StartDestination) {
        settingsRepository.setDefaultStartDestination(destination)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        settingsRepository.setDarkModeEnabled(enabled)
    }

    fun setNotificationQuickEntryEnabled(enabled: Boolean) {
        settingsRepository.setNotificationQuickEntryEnabled(enabled)
    }

    fun updateDailyReminder(hour: Int, minute: Int) {
        settingsRepository.updateDailyReminder(hour, minute)
    }

    fun disableDailyReminder() {
        settingsRepository.disableDailyReminder()
    }

    fun importData(
        uri: Uri,
        format: DataTransferFormat
    ) {
        viewModelScope.launch {
            _dataTransferState.value = DataTransferUiState(
                inProgress = true,
                message = "正在导入 ${format.displayName} 数据…",
                isError = false
            )

            runCatching {
                dataTransferRepository.importData(uri, format)
            }.onSuccess { summary ->
                _dataTransferState.value = DataTransferUiState(
                    inProgress = false,
                    message = buildString {
                        append("导入完成：")
                        append("新增分类 ${summary.insertedCategoryCount} 个，")
                        append("复用分类 ${summary.matchedCategoryCount} 个，")
                        append("新增流水 ${summary.insertedTransactionCount} 条，")
                        append("新增备注历史 ${summary.insertedNoteHistoryCount} 条，")
                        append("合并备注历史 ${summary.mergedNoteHistoryCount} 条，")
                        append("新增定时规则 ${summary.insertedRecurringRuleCount} 条，")
                        append("新增执行记录 ${summary.insertedRecurringExecutionCount} 条。")
                    },
                    isError = false
                )
            }.onFailure { error ->
                _dataTransferState.value = DataTransferUiState(
                    inProgress = false,
                    message = "导入失败：${error.message ?: "请检查文件内容和格式"}",
                    isError = true
                )
            }
        }
    }

    fun exportData(
        uri: Uri,
        format: DataTransferFormat
    ) {
        viewModelScope.launch {
            _dataTransferState.value = DataTransferUiState(
                inProgress = true,
                message = "正在导出 ${format.displayName} 数据…",
                isError = false
            )

            runCatching {
                dataTransferRepository.exportData(uri, format)
            }.onSuccess { summary ->
                _dataTransferState.value = DataTransferUiState(
                    inProgress = false,
                    message = "导出完成：分类 ${summary.categoryCount} 个，流水 ${summary.transactionCount} 条，备注历史 ${summary.noteHistoryCount} 条，定时规则 ${summary.recurringRuleCount} 条，执行记录 ${summary.recurringExecutionCount} 条。",
                    isError = false
                )
            }.onFailure { error ->
                _dataTransferState.value = DataTransferUiState(
                    inProgress = false,
                    message = "导出失败：${error.message ?: "无法写入目标文件"}",
                    isError = true
                )
            }
        }
    }

    fun clearDataTransferMessage() {
        if (_dataTransferState.value.inProgress) return
        _dataTransferState.value = DataTransferUiState()
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            dataTransferRepository: DataTransferRepository
        ) = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = settingsRepository,
                    dataTransferRepository = dataTransferRepository
                )
            }
        }
    }
}

data class DataTransferUiState(
    val inProgress: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)
