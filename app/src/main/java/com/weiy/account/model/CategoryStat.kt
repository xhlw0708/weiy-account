package com.weiy.account.model

data class CategoryStat(
    val categoryId: Long,
    val categoryName: String,
    val type: TransactionType,
    val totalAmount: Double
)

