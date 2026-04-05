package com.weiy.account.data.local.database

import androidx.room.TypeConverter
import com.weiy.account.model.TransactionType

class RoomConverters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}

