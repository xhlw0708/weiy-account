package com.weiy.account.data.local.entity

import androidx.room.Embedded

data class RecurringAccountingRuleWithCategory(
    @Embedded val rule: RecurringAccountingRuleEntity,
    @Embedded(prefix = "category_") private val categoryProjection: CategoryNameProjection
) {
    val categoryName: String
        get() = categoryProjection.categoryName
}

data class CategoryNameProjection(
    val categoryName: String
)
