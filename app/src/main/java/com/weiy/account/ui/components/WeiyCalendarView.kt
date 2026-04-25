package com.weiy.account.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class WeiyCalendarView(val label: String) {
    Day("日"),
    Week("周"),
    Month("月"),
    Year("年")
}

data class WeiyEntry(
    val id: String,
    val date: LocalDate,
    val title: String = "",
    val content: String = ""
)

@Composable
fun WeiyCalendar(
    entries: List<WeiyEntry>,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    view: WeiyCalendarView,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    startYear: Int = today.year - 5,
    onVisibleMonthChange: (YearMonth) -> Unit,
    onSelectedDateChange: (LocalDate) -> Unit,
    onViewChange: (WeiyCalendarView) -> Unit
) {
    val entriesByDate = remember(entries) { entries.groupBy { it.date } }
    val entriesByYear = remember(entries) { entries.groupBy { it.date.year } }
    val colorScheme = MaterialTheme.colorScheme
    val isTodaySelected = selectedDate == today && visibleMonth == YearMonth.from(today)
    val canGoNext = when (view) {
        WeiyCalendarView.Day,
        WeiyCalendarView.Week -> visibleMonth < YearMonth.from(today)

        WeiyCalendarView.Month,
        WeiyCalendarView.Year -> visibleMonth.year < today.year
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        WeiyCalendarTopBar(
            selectedView = view,
            isTodaySelected = isTodaySelected,
            onViewChange = onViewChange,
            onJumpToToday = {
                onVisibleMonthChange(YearMonth.from(today))
                onSelectedDateChange(today)
            }
        )

        Spacer(modifier = Modifier.height(26.dp))

        WeiyCalendarNavigator(
            visibleMonth = visibleMonth,
            view = view,
            canGoNext = canGoNext,
            onPrevious = {
                val next = when (view) {
                    WeiyCalendarView.Day,
                    WeiyCalendarView.Week -> visibleMonth.minusMonths(1)

                    WeiyCalendarView.Month,
                    WeiyCalendarView.Year -> visibleMonth.minusYears(1)
                }
                onVisibleMonthChange(next)
            },
            onNext = {
                if (!canGoNext) return@WeiyCalendarNavigator
                val next = when (view) {
                    WeiyCalendarView.Day,
                    WeiyCalendarView.Week -> visibleMonth.plusMonths(1)

                    WeiyCalendarView.Month,
                    WeiyCalendarView.Year -> visibleMonth.plusYears(1)
                }
                onVisibleMonthChange(next)
            }
        )

        Spacer(modifier = Modifier.height(22.dp))

        when (view) {
            WeiyCalendarView.Day -> WeiyDayGrid(
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                today = today,
                entriesByDate = entriesByDate,
                onDateClick = onSelectedDateChange
            )

            WeiyCalendarView.Week -> WeiyWeekGrid(
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                today = today,
                onWeekClick = { start, end ->
                    onSelectedDateChange(
                        if (today.isBetweenClosed(start, end)) today else start
                    )
                }
            )

            WeiyCalendarView.Month -> WeiyMonthGrid(
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                today = today,
                onMonthClick = { month ->
                    onVisibleMonthChange(month)
                    onSelectedDateChange(month.atDay(1))
                }
            )

            WeiyCalendarView.Year -> WeiyYearGrid(
                visibleYear = visibleMonth.year,
                selectedDate = selectedDate,
                today = today,
                startYear = startYear,
                entriesByYear = entriesByYear,
                onYearClick = { year ->
                    onVisibleMonthChange(YearMonth.of(year, 1))
                    onSelectedDateChange(LocalDate.of(year, 1, 1))
                }
            )
        }
    }
}

@Composable
private fun WeiyCalendarTopBar(
    selectedView: WeiyCalendarView,
    isTodaySelected: Boolean,
    onViewChange: (WeiyCalendarView) -> Unit,
    onJumpToToday: () -> Unit
) {
    val views = remember { enumValues<WeiyCalendarView>().toList() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeiyTodayButton(
            selected = isTodaySelected,
            onClick = onJumpToToday
        )

        Spacer(modifier = Modifier.width(12.dp))

        WeiySegmentedSwitch(
            modifier = Modifier.weight(1f),
            options = views.map { it.label },
            selectedIndex = views.indexOf(selectedView),
            onSelected = { index -> onViewChange(views[index]) }
        )
    }
}

@Composable
private fun WeiyCalendarNavigator(
    visibleMonth: YearMonth,
    view: WeiyCalendarView,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val title = when (view) {
        WeiyCalendarView.Day,
        WeiyCalendarView.Week -> "${visibleMonth.year}年 ${visibleMonth.monthValue}月"

        WeiyCalendarView.Month,
        WeiyCalendarView.Year -> "${visibleMonth.year}年"
    }
    val colorScheme = MaterialTheme.colorScheme
    val navigatorHeight = 44.dp
    val navigatorButtonSize = 44.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeiyNavButton(
            text = "‹",
            size = navigatorButtonSize,
            enabled = true,
            onClick = onPrevious
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .height(navigatorHeight)
                .width(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surface)
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        WeiyNavButton(
            text = "›",
            size = navigatorButtonSize,
            enabled = canGoNext,
            onClick = onNext
        )
    }
}

@Composable
private fun WeiyDayGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    entriesByDate: Map<LocalDate, List<WeiyEntry>>,
    onDateClick: (LocalDate) -> Unit
) {
    val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")
    val dates = remember(visibleMonth) { monthCellsSundayFirst(visibleMonth) }
    val colorScheme = MaterialTheme.colorScheme

    Row(modifier = Modifier.fillMaxWidth()) {
        weekLabels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = colorScheme.onSurface,
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    dates.chunked(7).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth()) {
            row.forEach { date ->
                WeiyDayCell(
                    date = date,
                    entryCount = date?.let { entriesByDate[it]?.size } ?: 0,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    onClick = { if (date != null) onDateClick(date) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WeiyDayCell(
    date: LocalDate?,
    entryCount: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
    ) {
        if (date == null) return@Box

        val hasEntry = entryCount > 0
        val isTodayNotWritten = isToday && !hasEntry
        val shape = RoundedCornerShape(8.dp)
        val colorScheme = MaterialTheme.colorScheme
        val mutedTextColor = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

        val bg = when {
            isSelected -> colorScheme.primary
            hasEntry -> colorScheme.primaryContainer
            isTodayNotWritten -> colorScheme.surfaceVariant
            else -> Color.Transparent
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .shadow(if (isSelected) 10.dp else 0.dp, shape)
                .clip(shape)
                .background(bg)
                .fastPressClickable(onClick = onClick)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isTodayNotWritten) "今" else date.dayOfMonth.toString(),
                color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            when {
                hasEntry -> Text(
                    text = "${entryCount}篇",
                    color = if (isSelected) colorScheme.onPrimary else colorScheme.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                isTodayNotWritten -> Text(
                    text = "未写",
                    color = mutedTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun WeiyWeekGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onWeekClick: (LocalDate, LocalDate) -> Unit
) {
    val weeks = remember(visibleMonth) { monthWeeksMondayToSunday(visibleMonth) }

    WeiyFixedGrid(
        items = weeks,
        columns = 3,
        horizontalSpacing = 8.dp,
        verticalSpacing = 8.dp
    ) { week ->
        val start = week.first
        val end = week.second

        WeiySummaryCard(
            title = if (today.isBetweenClosed(start, end)) "本周" else "${start.mmdd()}-${end.mmdd()}",
            subtitle = null,
            selected = selectedDate.isBetweenClosed(start, end),
            onClick = { onWeekClick(start, end) }
        )
    }
}

@Composable
private fun WeiyMonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onMonthClick: (YearMonth) -> Unit
) {
    val months = remember(visibleMonth.year) {
        (1..12).map { YearMonth.of(visibleMonth.year, it) }
    }

    WeiyFixedGrid(
        items = months,
        columns = 4,
        horizontalSpacing = 8.dp,
        verticalSpacing = 8.dp
    ) { month ->
        WeiySummaryCard(
            title = if (month == YearMonth.from(today)) "本月" else "${month.monthValue}月",
            subtitle = null,
            selected = month == YearMonth.from(selectedDate),
            onClick = { onMonthClick(month) }
        )
    }
}

@Composable
private fun WeiyYearGrid(
    visibleYear: Int,
    selectedDate: LocalDate,
    today: LocalDate,
    startYear: Int,
    entriesByYear: Map<Int, List<WeiyEntry>>,
    onYearClick: (Int) -> Unit
) {
    val endYear = maxOf(visibleYear, today.year)
    val years = remember(startYear, endYear) { (startYear..endYear).toList() }

    Spacer(modifier = Modifier.height(24.dp))

    WeiyFixedGrid(
        items = years,
        columns = 3,
        horizontalSpacing = 8.dp,
        verticalSpacing = 8.dp
    ) { year ->
        val count = entriesByYear[year]?.size ?: 0

        WeiySummaryCard(
            title = if (year == today.year) "本年" else "${year}年",
            subtitle = null,
            selected = year == selectedDate.year,
            disabled = count == 0,
            onClick = { onYearClick(year) }
        )
    }
}

@Composable
private fun WeiySummaryCard(
    title: String,
    subtitle: String?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    height: Dp = 108.dp,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(9.dp)
    val colorScheme = MaterialTheme.colorScheme
    val mutedTextColor = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val bg = when {
        selected -> colorScheme.primary
        disabled -> colorScheme.surfaceVariant
        else -> colorScheme.primaryContainer
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(if (selected) 10.dp else 0.dp, shape)
            .clip(shape)
            .background(bg)
            .fastPressClickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = if (selected) colorScheme.onPrimary else colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                color = when {
                    selected -> colorScheme.onPrimary
                    disabled -> mutedTextColor
                    else -> colorScheme.primary
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WeiySegmentedSwitch(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val selectedShape = RoundedCornerShape(22.dp)

    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colorScheme.surfaceVariant)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, text ->
            val selected = index == selectedIndex

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(selectedShape)
                    .background(if (selected) colorScheme.surface else Color.Transparent)
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) colorScheme.outlineVariant else Color.Transparent,
                        shape = selectedShape
                    )
                    .fastPressClickable { onSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WeiyTodayButton(
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = if (selected) colorScheme.primaryContainer else colorScheme.surfaceVariant
    val borderColor = if (selected) colorScheme.primary else colorScheme.outlineVariant
    val textColor = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .width(94.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .fastPressClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "今",
            color = textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WeiyNavButton(
    text: String,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = if (enabled) colorScheme.surface else colorScheme.surfaceVariant
    val borderColor = if (enabled) colorScheme.outlineVariant else colorScheme.outline.copy(alpha = 0.4f)
    val textColor = if (enabled) colorScheme.onSurfaceVariant else colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .fastPressClickable(
                enabled = enabled,
                showIndication = true,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
private fun <T> WeiyFixedGrid(
    items: List<T>,
    columns: Int,
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    itemContent: @Composable (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                repeat(columns) { index ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (index < rowItems.size) {
                            itemContent(rowItems[index])
                        }
                    }
                }
            }
        }
    }
}

private fun monthCellsSundayFirst(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingBlankCount = firstDay.dayOfWeek.sundayBasedIndex()
    val totalDays = month.lengthOfMonth()
    val totalCells = ((leadingBlankCount + totalDays + 6) / 7) * 7

    return (0 until totalCells).map { index ->
        val day = index - leadingBlankCount + 1
        if (day in 1..totalDays) month.atDay(day) else null
    }
}

private fun monthWeeksMondayToSunday(month: YearMonth): List<Pair<LocalDate, LocalDate>> {
    val firstWeekStart = month.atDay(1)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val monthEnd = month.atEndOfMonth()

    val weeks = mutableListOf<Pair<LocalDate, LocalDate>>()
    var start = firstWeekStart

    while (!start.isAfter(monthEnd)) {
        weeks += start to start.plusDays(6)
        start = start.plusWeeks(1)
    }

    return weeks
}

private fun DayOfWeek.sundayBasedIndex(): Int {
    return when (this) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
}

private fun LocalDate.isBetweenClosed(start: LocalDate, end: LocalDate): Boolean {
    return !isBefore(start) && !isAfter(end)
}

private fun LocalDate.mmdd(): String {
    return String.format(Locale.US, "%02d.%02d", monthValue, dayOfMonth)
}

private fun Modifier.fastPressClickable(
    enabled: Boolean = true,
    showIndication: Boolean = false,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val indication = if (showIndication) LocalIndication.current else null
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.97f else 1f,
        animationSpec = tween(
            durationMillis = if (pressed) 50 else 90
        ),
        label = "weiyCalendarFastPress"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = indication,
            onClick = onClick
        )
}

