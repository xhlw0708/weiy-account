package com.weiy.account.model

data class YearBill(
    val year: Int,
    val incomeTotal: Double,
    val expenseTotal: Double
) {
    val balance: Double
        get() = incomeTotal - expenseTotal
}
