package com.weiy.account.data.local.entity

import androidx.room.ColumnInfo
import com.weiy.account.model.TransactionType

data class CategoryStatRaw(
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    @ColumnInfo(name = "type")
    val type: TransactionType,
    @ColumnInfo(name = "total_amount")
    val totalAmount: Double
)

