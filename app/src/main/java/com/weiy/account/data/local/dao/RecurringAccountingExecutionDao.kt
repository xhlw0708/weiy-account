package com.weiy.account.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.weiy.account.data.local.entity.RecurringAccountingExecutionEntity

@Dao
interface RecurringAccountingExecutionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExecution(entity: RecurringAccountingExecutionEntity): Long

    @Update
    suspend fun updateExecution(entity: RecurringAccountingExecutionEntity)

    @Query(
        """
        SELECT *
        FROM recurring_accounting_executions
        WHERE ruleId = :ruleId
          AND occurrenceDateEpochDay = :occurrenceDateEpochDay
        LIMIT 1
        """
    )
    suspend fun getExecutionByRuleAndOccurrence(
        ruleId: Long,
        occurrenceDateEpochDay: Long
    ): RecurringAccountingExecutionEntity?

    @Query(
        """
        SELECT *
        FROM recurring_accounting_executions
        ORDER BY ruleId ASC, occurrenceDateEpochDay ASC
        """
    )
    suspend fun getAllExecutions(): List<RecurringAccountingExecutionEntity>
}
