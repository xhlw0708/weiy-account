package com.weiy.account.data.repository

import com.weiy.account.data.local.dao.CategoryDao
import com.weiy.account.data.local.entity.CategoryEntity
import com.weiy.account.model.CategoryItem
import com.weiy.account.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(
    private val categoryDao: CategoryDao
) {

    fun observeCategoriesByType(type: TransactionType): Flow<List<CategoryItem>> {
        return categoryDao.observeCategoriesByType(type).map { list -> list.map { it.toModel() } }
    }

    suspend fun getFirstCategoryIdByType(type: TransactionType): Long? {
        return categoryDao.getFirstCategoryByType(type)?.id
    }

    suspend fun addCategory(name: String, type: TransactionType) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val sortOrder = categoryDao.getMaxSortOrderByType(type) + 1
        categoryDao.insertCategory(
            CategoryEntity(
                name = trimmed,
                type = type,
                isDefault = false,
                sortOrder = sortOrder
            )
        )
    }

    suspend fun renameCategory(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        categoryDao.updateCategoryName(id, trimmed)
    }

    suspend fun ensureDefaultCategories() {
        if (categoryDao.countAll() > 0) return
        categoryDao.insertAll(defaultCategories())
    }

    private fun defaultCategories(): List<CategoryEntity> {
        val expenseDefaults = listOf(
            "餐饮",
            "交通",
            "购物",
            "日用",
            "住房",
            "娱乐",
            "医疗",
            "学习"
        )
        val incomeDefaults = listOf(
            "工资",
            "奖金",
            "兼职",
            "其他"
        )
        val categories = mutableListOf<CategoryEntity>()
        expenseDefaults.forEachIndexed { index, name ->
            categories += CategoryEntity(
                name = name,
                type = TransactionType.EXPENSE,
                isDefault = true,
                sortOrder = index
            )
        }
        incomeDefaults.forEachIndexed { index, name ->
            categories += CategoryEntity(
                name = name,
                type = TransactionType.INCOME,
                isDefault = true,
                sortOrder = index
            )
        }
        return categories
    }
}

private fun CategoryEntity.toModel(): CategoryItem {
    return CategoryItem(
        id = id,
        name = name,
        type = type,
        iconKey = iconKey,
        isDefault = isDefault,
        sortOrder = sortOrder
    )
}

