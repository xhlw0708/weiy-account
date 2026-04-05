package com.weiy.account.model

data class CategoryItem(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val iconKey: String?,
    val isDefault: Boolean,
    val sortOrder: Int
)

