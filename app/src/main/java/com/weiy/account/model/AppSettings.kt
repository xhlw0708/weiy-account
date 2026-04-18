package com.weiy.account.model

data class AppSettings(
    val onboardingShown: Boolean = false,
    val defaultTransactionType: TransactionType = TransactionType.EXPENSE,
    val defaultStartDestination: StartDestination = StartDestination.HOME,
    val darkModeEnabled: Boolean = false,
    val notificationQuickEntryEnabled: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0
)
