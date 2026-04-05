package com.weiy.account.model

enum class TransactionType {
    INCOME,
    EXPENSE;

    val displayName: String
        get() = when (this) {
            INCOME -> "收入"
            EXPENSE -> "支出"
        }
}

