package com.weiy.account.model

data class MonthBill(
    val month: Int,
    val incomeTotal: Double,
    val expenseTotal: Double
) {
    val balance: Double
        get() = incomeTotal - expenseTotal
}
