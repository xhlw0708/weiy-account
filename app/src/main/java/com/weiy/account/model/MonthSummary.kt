package com.weiy.account.model

data class MonthSummary(
    val incomeTotal: Double = 0.0,
    val expenseTotal: Double = 0.0
) {
    val balance: Double
        get() = incomeTotal - expenseTotal
}

