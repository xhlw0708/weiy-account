package com.weiy.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.weiy.account.data.repository.SettingsRepository
import com.weiy.account.model.StartDestination
import com.weiy.account.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<com.weiy.account.model.AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = settingsRepository.currentSettings
    )

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

    companion object {
        fun factory(settingsRepository: SettingsRepository) = viewModelFactory {
            initializer {
                SettingsViewModel(settingsRepository)
            }
        }
    }
}

