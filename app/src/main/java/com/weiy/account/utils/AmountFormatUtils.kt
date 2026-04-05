package com.weiy.account.utils

import java.text.DecimalFormat

private val amountFormatter = DecimalFormat("#,##0.00")

fun formatAmount(amount: Double): String = amountFormatter.format(amount)

