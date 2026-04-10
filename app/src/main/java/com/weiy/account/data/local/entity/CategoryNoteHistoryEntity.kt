package com.weiy.account.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_note_history",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("categoryId"),
        Index(value = ["categoryId", "note"], unique = true)
    ]
)
data class CategoryNoteHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val categoryId: Long,
    val note: String,
    val usageCount: Int,
    val lastUsedAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)
