package com.weiy.account.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.weiy.account.model.TransactionType

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val type: TransactionType,
    val iconKey: String? = null,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

