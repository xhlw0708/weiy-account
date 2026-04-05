package com.weiy.account.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val zoneId: ZoneId = ZoneId.systemDefault()
private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")
private val dateHeaderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun currentYearMonth(): YearMonth = YearMonth.now()

fun monthStartMillis(yearMonth: YearMonth): Long {
    return yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
}

fun monthEndMillis(yearMonth: YearMonth): Long {
    return yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
}

fun yearStartMillis(year: Int): Long {
    return Year.of(year).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
}

fun yearEndMillis(year: Int): Long {
    return Year.of(year + 1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
}

fun formatMonth(yearMonth: YearMonth): String = yearMonth.format(monthFormatter)

fun formatDateHeader(timestampMillis: Long): String {
    val date = Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDate()
    return date.format(dateHeaderFormatter)
}

fun formatDate(timestampMillis: Long): String {
    val date = Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDate()
    return date.format(dateFormatter)
}

fun formatTime(timestampMillis: Long): String {
    val time = Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalTime()
    return time.format(timeFormatter)
}

fun formatDateTime(timestampMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDateTime()
    return dateTime.format(dateTimeFormatter)
}

fun toLocalDate(timestampMillis: Long): LocalDate {
    return Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDate()
}

fun toLocalDateTime(timestampMillis: Long): LocalDateTime {
    return Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDateTime()
}

fun toMillis(localDateTime: LocalDateTime): Long {
    return localDateTime.atZone(zoneId).toInstant().toEpochMilli()
}
