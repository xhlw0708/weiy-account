package com.weiy.account.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_accounting_executions",
    foreignKeys = [
        ForeignKey(
            entity = RecurringAccountingRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("ruleId"),
        Index(value = ["ruleId", "occurrenceDateEpochDay"], unique = true)
    ]
)
data class RecurringAccountingExecutionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val ruleId: Long,
    val occurrenceDateEpochDay: Long,
    val generatedTransactionId: Long?,
    val executedAt: Long,
    val createdAt: Long
)
