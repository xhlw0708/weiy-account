package com.weiy.account.reminder

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountingReminderSchedulerTest {

    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun calculateNextTriggerAtMillis_usesSameDayWhenTimeHasNotPassed() {
        val now = ZonedDateTime.of(2026, 4, 12, 20, 30, 0, 0, zoneId)

        val triggerAtMillis = AccountingReminderScheduler.calculateNextTriggerAtMillis(
            now = now,
            hour = 21,
            minute = 0
        )

        assertEquals(
            ZonedDateTime.of(2026, 4, 12, 21, 0, 0, 0, zoneId),
            Instant.ofEpochMilli(triggerAtMillis).atZone(zoneId)
        )
    }

    @Test
    fun calculateNextTriggerAtMillis_movesToNextDayWhenTimeHasPassed() {
        val now = ZonedDateTime.of(2026, 4, 12, 21, 1, 0, 0, zoneId)

        val triggerAtMillis = AccountingReminderScheduler.calculateNextTriggerAtMillis(
            now = now,
            hour = 21,
            minute = 0
        )

        assertEquals(
            ZonedDateTime.of(2026, 4, 13, 21, 0, 0, 0, zoneId),
            Instant.ofEpochMilli(triggerAtMillis).atZone(zoneId)
        )
    }

    @Test
    fun calculateNextTriggerAtMillis_movesToNextDayWhenTimeIsExactlyNow() {
        val now = ZonedDateTime.of(2026, 4, 12, 21, 0, 0, 0, zoneId)

        val triggerAtMillis = AccountingReminderScheduler.calculateNextTriggerAtMillis(
            now = now,
            hour = 21,
            minute = 0
        )

        assertEquals(
            ZonedDateTime.of(2026, 4, 13, 21, 0, 0, 0, zoneId),
            Instant.ofEpochMilli(triggerAtMillis).atZone(zoneId)
        )
    }
}
