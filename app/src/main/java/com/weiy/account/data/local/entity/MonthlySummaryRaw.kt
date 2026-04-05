package com.weiy.account.data.local.entity

import androidx.room.ColumnInfo

data class MonthlySummaryRaw(
    @ColumnInfo(name = "income_total")
    val incomeTotal: Double,
    @ColumnInfo(name = "expense_total")
    val expenseTotal: Double
)

