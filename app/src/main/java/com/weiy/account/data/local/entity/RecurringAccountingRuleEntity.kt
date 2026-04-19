package com.weiy.account.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.weiy.account.model.RecurrenceUnit
import com.weiy.account.model.TransactionType

@Entity(
    tableName = "recurring_accounting_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("categoryId"),
        Index("enabled"),
        Index("nextDueDateEpochDay")
    ]
)
data class RecurringAccountingRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long,
    val note: String,
    val tagsSerialized: String,
    val firstOccurrenceDateEpochDay: Long,
    val repeatUnit: RecurrenceUnit,
    val repeatInterval: Int,
    val enabled: Boolean,
    val lastExecutedDateEpochDay: Long?,
    val nextDueDateEpochDay: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
