package com.weiy.account.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.weiy.account.model.TransactionType

data class TransactionWithCategory(
    @Embedded
    val transaction: TransactionEntity,
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    @ColumnInfo(name = "category_type")
    val categoryType: TransactionType
)

