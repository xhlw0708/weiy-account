package com.weiy.account.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.weiy.account.data.local.entity.RecurringAccountingRuleEntity
import com.weiy.account.data.local.entity.RecurringAccountingRuleWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringAccountingRuleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRule(entity: RecurringAccountingRuleEntity): Long

    @Update
    suspend fun updateRule(entity: RecurringAccountingRuleEntity)

    @Query(
        """
        UPDATE recurring_accounting_rules
        SET enabled = :enabled,
            updatedAt = :updatedAt
        WHERE id = :ruleId
        """
    )
    suspend fun updateEnabled(
        ruleId: Long,
        enabled: Boolean,
        updatedAt: Long
    )

    @Query("DELETE FROM recurring_accounting_rules WHERE id = :ruleId")
    suspend fun deleteRule(ruleId: Long)

    @Query(
        """
        SELECT *
        FROM recurring_accounting_rules
        WHERE id = :ruleId
        LIMIT 1
        """
    )
    suspend fun getRuleById(ruleId: Long): RecurringAccountingRuleEntity?

    @Query(
        """
        SELECT r.*, c.name AS category_categoryName
        FROM recurring_accounting_rules r
        INNER JOIN categories c ON r.categoryId = c.id
        ORDER BY r.createdAt DESC, r.id DESC
        """
    )
    fun observeRulesWithCategory(): Flow<List<RecurringAccountingRuleWithCategory>>

    @Query(
        """
        SELECT *
        FROM recurring_accounting_rules
        WHERE enabled = 1
        ORDER BY createdAt ASC, id ASC
        """
    )
    suspend fun getEnabledRules(): List<RecurringAccountingRuleEntity>

    @Query(
        """
        UPDATE recurring_accounting_rules
        SET lastExecutedDateEpochDay = :lastExecutedDateEpochDay,
            nextDueDateEpochDay = :nextDueDateEpochDay,
            updatedAt = :updatedAt
        WHERE id = :ruleId
        """
    )
    suspend fun updateExecutionPointers(
        ruleId: Long,
        lastExecutedDateEpochDay: Long?,
        nextDueDateEpochDay: Long?,
        updatedAt: Long
    )

    @Query(
        """
        SELECT *
        FROM recurring_accounting_rules
        ORDER BY createdAt ASC, id ASC
        """
    )
    suspend fun getAllRules(): List<RecurringAccountingRuleEntity>
}
