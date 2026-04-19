package com.weiy.account.data.repository

import androidx.room.withTransaction
import com.weiy.account.data.local.database.AppDatabase
import com.weiy.account.data.local.entity.RecurringAccountingExecutionEntity
import com.weiy.account.data.local.entity.RecurringAccountingRuleEntity
import com.weiy.account.model.RecurrenceUnit
import com.weiy.account.model.RecurringAccountingRuleItem
import com.weiy.account.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecurringAccountingRepository(
    private val database: AppDatabase,
    private val transactionRepository: TransactionRepository
) {
    private val ruleDao = database.recurringAccountingRuleDao()
    private val executionDao = database.recurringAccountingExecutionDao()

    fun observeRules(): Flow<List<RecurringAccountingRuleItem>> {
        return ruleDao.observeRulesWithCategory().map { list ->
            list.map { item ->
                RecurringAccountingRuleItem(
                    id = item.rule.id,
                    type = item.rule.type,
                    amount = item.rule.amount,
                    categoryId = item.rule.categoryId,
                    categoryName = item.categoryName,
                    note = item.rule.note,
                    firstOccurrenceDateEpochDay = item.rule.firstOccurrenceDateEpochDay,
                    repeatUnit = item.rule.repeatUnit,
                    enabled = item.rule.enabled,
                    lastExecutedDateEpochDay = item.rule.lastExecutedDateEpochDay,
                    nextDueDateEpochDay = item.rule.nextDueDateEpochDay
                )
            }
        }
    }

    suspend fun addRule(
        type: TransactionType,
        amount: Double,
        categoryId: Long,
        note: String,
        firstOccurrenceDateEpochDay: Long,
        repeatUnit: RecurrenceUnit,
        enabled: Boolean = true
    ): Long {
        val now = System.currentTimeMillis()
        val nextDue = calculateNextDueDate(
            baseDateEpochDay = firstOccurrenceDateEpochDay,
            repeatUnit = repeatUnit,
            fromDateEpochDay = firstOccurrenceDateEpochDay
        )
        return ruleDao.insertRule(
            RecurringAccountingRuleEntity(
                type = type,
                amount = amount,
                categoryId = categoryId,
                note = note.trim(),
                tagsSerialized = "",
                firstOccurrenceDateEpochDay = firstOccurrenceDateEpochDay,
                repeatUnit = repeatUnit,
                repeatInterval = 1,
                enabled = enabled,
                lastExecutedDateEpochDay = null,
                nextDueDateEpochDay = nextDue,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateRuleEnabled(
        ruleId: Long,
        enabled: Boolean
    ) {
        ruleDao.updateEnabled(
            ruleId = ruleId,
            enabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun runStartupBackfill(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        val todayEpochDay = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), zoneId).toEpochDay()
        val rules = ruleDao.getEnabledRules()
        rules.forEach { rule ->
            runRuleBackfill(rule, todayEpochDay, zoneId)
        }
    }

    private suspend fun runRuleBackfill(
        rule: RecurringAccountingRuleEntity,
        todayEpochDay: Long,
        zoneId: ZoneId
    ) {
        var cursor = rule.lastExecutedDateEpochDay?.plus(1)
            ?: rule.firstOccurrenceDateEpochDay
        if (cursor < rule.firstOccurrenceDateEpochDay) {
            cursor = rule.firstOccurrenceDateEpochDay
        }

        var lastExecuted: Long? = rule.lastExecutedDateEpochDay
        while (cursor <= todayEpochDay) {
            val inserted = executeOccurrenceIfNeeded(
                rule = rule,
                occurrenceDateEpochDay = cursor,
                zoneId = zoneId
            )
            if (inserted) {
                lastExecuted = cursor
            }
            cursor = calculateNextDueDate(
                baseDateEpochDay = rule.firstOccurrenceDateEpochDay,
                repeatUnit = rule.repeatUnit,
                fromDateEpochDay = cursor
            ) ?: break
        }

        val nextDue = if (cursor > todayEpochDay) cursor else calculateNextDueDate(
            baseDateEpochDay = rule.firstOccurrenceDateEpochDay,
            repeatUnit = rule.repeatUnit,
            fromDateEpochDay = maxOf(todayEpochDay, lastExecuted ?: todayEpochDay)
        )

        ruleDao.updateExecutionPointers(
            ruleId = rule.id,
            lastExecutedDateEpochDay = lastExecuted,
            nextDueDateEpochDay = nextDue,
            updatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun executeOccurrenceIfNeeded(
        rule: RecurringAccountingRuleEntity,
        occurrenceDateEpochDay: Long,
        zoneId: ZoneId
    ): Boolean {
        val executionId = database.withTransaction {
            executionDao.insertExecution(
                RecurringAccountingExecutionEntity(
                    ruleId = rule.id,
                    occurrenceDateEpochDay = occurrenceDateEpochDay,
                    generatedTransactionId = null,
                    executedAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        if (executionId <= 0L) return false

        val dateTime = LocalDate.ofEpochDay(occurrenceDateEpochDay)
            .atTime(12, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val transactionId = transactionRepository.addTransaction(
            type = rule.type,
            amount = rule.amount,
            categoryId = rule.categoryId,
            note = rule.note,
            dateTime = dateTime
        )

        database.withTransaction {
            val execution = executionDao.getExecutionByRuleAndOccurrence(
                ruleId = rule.id,
                occurrenceDateEpochDay = occurrenceDateEpochDay
            ) ?: return@withTransaction
            executionDao.updateExecution(
                execution.copy(generatedTransactionId = transactionId)
            )
        }
        return true
    }

    private fun calculateNextDueDate(
        baseDateEpochDay: Long,
        repeatUnit: RecurrenceUnit,
        fromDateEpochDay: Long
    ): Long? {
        val base = LocalDate.ofEpochDay(baseDateEpochDay)
        val from = LocalDate.ofEpochDay(fromDateEpochDay)
        val next = when (repeatUnit) {
            RecurrenceUnit.DAILY -> from.plusDays(1)
            RecurrenceUnit.WEEKLY -> from.plusWeeks(1)
            RecurrenceUnit.MONTHLY -> {
                val nextMonth = from.plusMonths(1)
                nextMonth.withDayOfMonth(minOf(base.dayOfMonth, nextMonth.lengthOfMonth()))
            }
            RecurrenceUnit.YEARLY -> {
                val nextYear = from.plusYears(1)
                val dayOfMonth = minOf(base.dayOfMonth, nextYear.lengthOfMonth())
                nextYear.withDayOfMonth(dayOfMonth)
            }
        }
        return next.toEpochDay()
    }
}
