package com.weiy.account.model

data class AppSettings(
    val onboardingShown: Boolean = false,
    val defaultTransactionType: TransactionType = TransactionType.EXPENSE,
    val defaultStartDestination: StartDestination = StartDestination.HOME,
    val darkModeEnabled: Boolean = false
)

