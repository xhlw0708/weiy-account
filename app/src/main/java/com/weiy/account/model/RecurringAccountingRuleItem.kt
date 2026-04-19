package com.weiy.account.model

data class RecurringAccountingRuleItem(
    val id: Long,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val note: String,
    val firstOccurrenceDateEpochDay: Long,
    val repeatUnit: RecurrenceUnit,
    val enabled: Boolean,
    val lastExecutedDateEpochDay: Long?,
    val nextDueDateEpochDay: Long?
)
