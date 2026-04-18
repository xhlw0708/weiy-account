package com.weiy.account.data.repository

import android.content.SharedPreferences
import com.weiy.account.model.AppSettings
import com.weiy.account.model.BillYearRange
import com.weiy.account.model.StartDestination
import com.weiy.account.model.TransactionType
import com.weiy.account.preferences.SettingsPreferencesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(
    private val dataSource: SettingsPreferencesDataSource
) {

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settings.value = dataSource.readSettings()
        _billYearRange.value = dataSource.readBillYearRange()
    }

    private val _settings = MutableStateFlow(dataSource.readSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    private val _billYearRange = MutableStateFlow(dataSource.readBillYearRange())
    val billYearRange: StateFlow<BillYearRange> = _billYearRange.asStateFlow()

    val currentSettings: AppSettings
        get() = settings.value
    val currentBillYearRange: BillYearRange
        get() = billYearRange.value

    init {
        dataSource.registerListener(preferenceChangeListener)
    }

    fun setOnboardingShown(shown: Boolean) {
        dataSource.setOnboardingShown(shown)
        _settings.value = dataSource.readSettings()
    }

    fun setDefaultTransactionType(type: TransactionType) {
        dataSource.setDefaultTransactionType(type)
        _settings.value = dataSource.readSettings()
    }

    fun setDefaultStartDestination(destination: StartDestination) {
        dataSource.setDefaultStartDestination(destination)
        _settings.value = dataSource.readSettings()
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        dataSource.setDarkModeEnabled(enabled)
        _settings.value = dataSource.readSettings()
    }

    fun setNotificationQuickEntryEnabled(enabled: Boolean) {
        dataSource.setNotificationQuickEntryEnabled(enabled)
        _settings.value = dataSource.readSettings()
    }

    fun updateDailyReminder(hour: Int, minute: Int) {
        dataSource.updateDailyReminder(hour, minute)
        _settings.value = dataSource.readSettings()
    }

    fun disableDailyReminder() {
        dataSource.disableDailyReminder()
        _settings.value = dataSource.readSettings()
    }

    fun refreshBillYearRange() {
        _billYearRange.value = dataSource.readBillYearRange()
    }
}
