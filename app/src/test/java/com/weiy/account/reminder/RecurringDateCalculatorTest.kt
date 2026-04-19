package com.weiy.account.reminder

import com.weiy.account.model.RecurrenceUnit
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringDateCalculatorTest {

    @Test
    fun monthlyRule_usesMonthEndWhenDayDoesNotExist() {
        val first = LocalDate.of(2026, 1, 31)
        val from = LocalDate.of(2026, 1, 31)

        val next = calculateNextDueDate(
            baseDateEpochDay = first.toEpochDay(),
            repeatUnit = RecurrenceUnit.MONTHLY,
            fromDateEpochDay = from.toEpochDay()
        )

        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), next)
    }

    @Test
    fun yearlyRule_handlesLeapDay() {
        val first = LocalDate.of(2024, 2, 29)
        val from = LocalDate.of(2024, 2, 29)

        val next = calculateNextDueDate(
            baseDateEpochDay = first.toEpochDay(),
            repeatUnit = RecurrenceUnit.YEARLY,
            fromDateEpochDay = from.toEpochDay()
        )

        assertEquals(LocalDate.of(2025, 2, 28).toEpochDay(), next)
    }

    private fun calculateNextDueDate(
        baseDateEpochDay: Long,
        repeatUnit: RecurrenceUnit,
        fromDateEpochDay: Long
    ): Long {
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
