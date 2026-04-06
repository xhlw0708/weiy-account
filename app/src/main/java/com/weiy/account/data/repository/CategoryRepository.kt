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

    suspend fun deleteCategory(category: CategoryItem): String? {
        if (category.isDefault) {
            return "默认分类不可删除"
        }
        if (categoryDao.countTransactionsByCategoryId(category.id) > 0) {
            return "该分类下已有账目，无法删除"
        }
        categoryDao.deleteCategoryById(category.id)
        return null
    }

    suspend fun ensureDefaultCategories() {
        categoryDao.insertAll(defaultCategories())
    }

    private fun defaultCategories(): List<CategoryEntity> {
        val expenseDefaults = listOf(
            "餐饮" to "utensils",
            "购物" to "bag",
            "日用" to "briefcase",
            "交通" to "transport",
            "蔬菜" to "produce",
            "水果" to "fruit",
            "零食" to "snack",
            "运动" to "sport",
            "娱乐" to "game",
            "通讯" to "phone",
            "服饰" to "shirt",
            "美容" to "sparkle",
            "住房" to "home",
            "居家" to "sofa",
            "长辈" to "family",
            "旅行" to "travel",
            "烟酒" to "glass",
            "数码" to "device",
            "医疗" to "medical",
            "书籍" to "book",
            "学习" to "cap",
            "宠物" to "paw",
            "礼金" to "money",
            "礼物" to "gift",
            "办公" to "briefcase",
            "亲友" to "family",
            "快递" to "box",
            "设置" to "settings"
        )
        val incomeDefaults = listOf(
            "工资" to "salary",
            "兼职" to "clock_money",
            "礼金" to "money",
            "其他" to "money_bag",
            "回收" to "recycle",
            "补贴" to "bonus",
            "设置" to "settings"
        )
        val categories = mutableListOf<CategoryEntity>()
        expenseDefaults.forEachIndexed { index, (name, iconKey) ->
            categories += CategoryEntity(
                name = name,
                type = TransactionType.EXPENSE,
                iconKey = iconKey,
                isDefault = true,
                sortOrder = index
            )
        }
        incomeDefaults.forEachIndexed { index, (name, iconKey) ->
            categories += CategoryEntity(
                name = name,
                type = TransactionType.INCOME,
                iconKey = iconKey,
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
