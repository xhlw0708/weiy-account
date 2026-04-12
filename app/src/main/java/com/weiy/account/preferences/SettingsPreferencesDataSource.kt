package com.weiy.account.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.weiy.account.model.AppSettings
import com.weiy.account.model.BillYearRange
import com.weiy.account.model.StartDestination
import com.weiy.account.model.TransactionType
import java.time.LocalDate

class SettingsPreferencesDataSource(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readSettings(): AppSettings {
        val onboardingShown = preferences.getBoolean(KEY_ONBOARDING_SHOWN, false)
        val defaultType = preferences.getString(KEY_DEFAULT_TRANSACTION_TYPE, TransactionType.EXPENSE.name)
            ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
            ?: TransactionType.EXPENSE
        val defaultStart = preferences.getString(KEY_DEFAULT_START_DESTINATION, StartDestination.HOME.name)
            ?.let { runCatching { StartDestination.valueOf(it) }.getOrNull() }
            ?: StartDestination.HOME
        val darkMode = preferences.getBoolean(KEY_DARK_MODE_ENABLED, false)
        val reminderEnabled = preferences.getBoolean(KEY_REMINDER_ENABLED, false)
        val reminderHour = preferences.getInt(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR)
            .coerceIn(0, 23)
        val reminderMinute = preferences.getInt(KEY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE)
            .coerceIn(0, 59)
        return AppSettings(
            onboardingShown = onboardingShown,
            defaultTransactionType = defaultType,
            defaultStartDestination = defaultStart,
            darkModeEnabled = darkMode,
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute
        )
    }

    fun setOnboardingShown(value: Boolean) {
        preferences.edit { putBoolean(KEY_ONBOARDING_SHOWN, value) }
    }

    fun setDefaultTransactionType(type: TransactionType) {
        preferences.edit { putString(KEY_DEFAULT_TRANSACTION_TYPE, type.name) }
    }

    fun setDefaultStartDestination(destination: StartDestination) {
        preferences.edit { putString(KEY_DEFAULT_START_DESTINATION, destination.name) }
    }

    fun setDarkModeEnabled(value: Boolean) {
        preferences.edit { putBoolean(KEY_DARK_MODE_ENABLED, value) }
    }

    fun updateDailyReminder(hour: Int, minute: Int) {
        preferences.edit {
            putBoolean(KEY_REMINDER_ENABLED, true)
            putInt(KEY_REMINDER_HOUR, hour.coerceIn(0, 23))
            putInt(KEY_REMINDER_MINUTE, minute.coerceIn(0, 59))
        }
    }

    fun disableDailyReminder() {
        preferences.edit { putBoolean(KEY_REMINDER_ENABLED, false) }
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun isDemoData2026FebMarSeeded(): Boolean {
        return preferences.getBoolean(KEY_DEMO_DATA_2026_FEB_MAR_SEEDED, false)
    }

    fun setDemoData2026FebMarSeeded(seedDone: Boolean) {
        preferences.edit { putBoolean(KEY_DEMO_DATA_2026_FEB_MAR_SEEDED, seedDone) }
    }

    fun readBillYearRange(): BillYearRange {
        val currentYear = LocalDate.now().year
        val storedMinYear = preferences.getInt(KEY_MIN_BILL_YEAR, DEFAULT_MIN_BILL_YEAR)
        val storedMaxYear = preferences.getInt(KEY_MAX_BILL_YEAR, currentYear)

        val normalizedMinYear = DEFAULT_MIN_BILL_YEAR
        val normalizedMaxYear = maxOf(currentYear, normalizedMinYear)

        if (storedMinYear != normalizedMinYear || storedMaxYear != normalizedMaxYear) {
            preferences.edit {
                putInt(KEY_MIN_BILL_YEAR, normalizedMinYear)
                putInt(KEY_MAX_BILL_YEAR, normalizedMaxYear)
            }
        }

        return BillYearRange(
            minYear = normalizedMinYear,
            maxYear = normalizedMaxYear
        )
    }

    companion object {
        private const val PREFS_NAME = "account_settings"
        const val KEY_ONBOARDING_SHOWN = "onboardingShown"
        const val KEY_DEFAULT_TRANSACTION_TYPE = "defaultTransactionType"
        const val KEY_DEFAULT_START_DESTINATION = "defaultStartDestination"
        const val KEY_DARK_MODE_ENABLED = "darkModeEnabled"
        const val KEY_REMINDER_ENABLED = "reminderEnabled"
        const val KEY_REMINDER_HOUR = "reminderHour"
        const val KEY_REMINDER_MINUTE = "reminderMinute"
        const val KEY_MIN_BILL_YEAR = "minBillYear"
        const val KEY_MAX_BILL_YEAR = "maxBillYear"
        const val KEY_DEMO_DATA_2026_FEB_MAR_SEEDED = "demoData2026FebMarSeeded"
        private const val DEFAULT_MIN_BILL_YEAR = 2002
        private const val DEFAULT_REMINDER_HOUR = 21
        private const val DEFAULT_REMINDER_MINUTE = 0
    }
}
