package com.weiy.account.model

data class TransactionRecord(
    val id: Long,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val note: String,
    val dateTime: Long,
    val createdAt: Long,
    val updatedAt: Long
)

