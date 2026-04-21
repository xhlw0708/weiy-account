package com.weiy.account.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weiy.account.R
import com.weiy.account.model.TransactionType
import com.weiy.account.utils.formatAmount
import com.weiy.account.utils.toLocalDate
import com.weiy.account.viewmodel.MonthBillDetailUiState
import com.weiy.account.viewmodel.MonthBillDetailViewModel
import com.weiy.account.viewmodel.MonthCategoryChangeItem
import com.weiy.account.viewmodel.MonthCategoryRatioItem
import com.weiy.account.viewmodel.MonthCompareBarItem
import com.weiy.account.viewmodel.MonthExpenseRankItem
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val rankDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")
private val rankListDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM月dd日")

@Composable
fun MonthBillDetailScreen(
    viewModel: MonthBillDetailViewModel,
    onOpenExpenseRank: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BalanceSummaryCard(uiState = uiState)
        }

        item {
            ExpenseCategoryCard(
                uiState = uiState,
                onOpenExpenseRank = {
                    onOpenExpenseRank(uiState.yearMonth.year, uiState.yearMonth.monthValue)
                }
            )
        }

        item {
            ExpenseTrendCard(uiState = uiState)
        }

        item {
            ExpenseCompareCard(uiState = uiState)
        }

        item {
            IncomeCard(uiState = uiState)
        }
    }
}

@Composable
private fun BalanceSummaryCard(uiState: MonthBillDetailUiState) {
    val maxAmount = maxOf(
        uiState.currentSummary.incomeTotal,
        uiState.currentSummary.expenseTotal,
        1.0
    )

    DetailCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "本月结余",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatAmount(uiState.currentSummary.balance),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "上月结余",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatAmount(uiState.previousSummary.balance),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        AmountProgressRow(
            label = "支出",
            amount = uiState.currentSummary.expenseTotal,
            maxAmount = maxAmount
        )
        Spacer(modifier = Modifier.height(10.dp))
        AmountProgressRow(
            label = "收入",
            amount = uiState.currentSummary.incomeTotal,
            maxAmount = maxAmount
        )
    }
}

@Composable
private fun AmountProgressRow(
    label: String,
    amount: Double,
    maxAmount: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(34.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { (amount / maxAmount).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatAmount(amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MonthExpenseRankScreen(
    viewModel: MonthBillDetailViewModel,
    searchTriggerKey: Int,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearchField by rememberSaveable { mutableStateOf(false) }
    var searchKeyword by rememberSaveable { mutableStateOf("") }
    var lastHandledSearchTrigger by rememberSaveable { mutableStateOf(searchTriggerKey) }
    val filteredRanking = remember(uiState.expenseRankingAll, searchKeyword) {
        if (searchKeyword.isBlank()) {
            uiState.expenseRankingAll
        } else {
            val keyword = searchKeyword.trim()
            uiState.expenseRankingAll.filter { item ->
                item.categoryName.contains(keyword, ignoreCase = true)
            }
        }
    }
    LaunchedEffect(searchTriggerKey) {
        if (searchTriggerKey > lastHandledSearchTrigger) {
            showSearchField = true
            lastHandledSearchTrigger = searchTriggerKey
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (showSearchField) {
            item {
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = { searchKeyword = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    placeholder = {
                        Text(text = "搜索类别")
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清空搜索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                showSearchField = false
                                searchKeyword = ""
                            }
                        )
                    }
                )
            }
        }

        if (uiState.expenseRankingAll.isEmpty()) {
            item {
                Text(
                    text = "暂无支出排行",
                    modifier = Modifier.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (filteredRanking.isEmpty()) {
            item {
                Text(
                    text = "未找到匹配的支出排行",
                    modifier = Modifier.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            itemsIndexed(filteredRanking) { _, item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryIconSymbol(
                                name = item.categoryName,
                                type = TransactionType.EXPENSE,
                                iconKey = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        ) {
                            Text(
                                text = item.categoryName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = toLocalDate(item.dateTime).format(rankListDateFormatter),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "-${formatAmount(item.amount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun ExpenseCategoryCard(
    uiState: MonthBillDetailUiState,
    onOpenExpenseRank: () -> Unit
) {
    DetailCard {
        Text(
            text = "支出类别",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.expenseCategories.isEmpty()) {
            Text(
                text = "暂无支出数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val pieColors = piePalette()
            val pieData = uiState.expenseCategories.mapIndexed { index, item ->
                val color = pieColors[index % pieColors.size]
                Pie(
                    label = item.categoryName,
                    data = item.amount,
                    color = color,
                    selectedColor = color.copy(alpha = 0.8f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PieChart(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.CenterVertically),
                    data = pieData,
                    style = Pie.Style.Stroke(),
                    selectedScale = 1.04f,
                    spaceDegree = 2f,
                    selectedPaddingDegree = 2f
                )

                ExpenseLegend(
                    items = uiState.expenseCategories,
                    colors = pieColors,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "支出排行",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    modifier = Modifier.clickable {
                        onOpenExpenseRank()
                    },
                    text = "查看更多",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.expenseRanking.isEmpty()) {
                Text(
                    text = "暂无支出排行",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.expenseRanking.forEachIndexed { index, item ->
                        ExpenseRankRow(
                            rank = index + 1,
                            item = item
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseLegend(
    items: List<MonthCategoryRatioItem>,
    colors: List<androidx.compose.ui.graphics.Color>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "类别",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "占比",
                modifier = Modifier.width(58.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "金额",
                modifier = Modifier.width(76.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors[index % colors.size])
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.categoryName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatPercent(item.ratio),
                    modifier = Modifier.width(58.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatAmount(item.amount),
                    modifier = Modifier.width(76.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ExpenseRankRow(
    rank: Int,
    item: MonthExpenseRankItem
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            modifier = Modifier.width(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            CategoryIconSymbol(
                name = item.categoryName,
                type = TransactionType.EXPENSE,
                iconKey = null,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = item.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = toLocalDate(item.dateTime).format(rankDateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "-${formatAmount(item.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ExpenseTrendCard(uiState: MonthBillDetailUiState) {
    DetailCard {
        Text(
            text = "支出趋势",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TrendMetric(
                title = "单日支出最高",
                value = formatAmount(uiState.maxExpenseDayAmount),
                hint = uiState.maxExpenseDay?.let { "${uiState.yearMonth.monthValue}月${it}日" } ?: "--"
            )
            TrendMetric(
                title = "日均支出",
                value = formatAmount(uiState.averageDailyExpense),
                hint = ""
            )
            TrendMetric(
                title = "本月支出",
                value = formatAmount(uiState.currentSummary.expenseTotal),
                hint = ""
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        ExpenseTrendChart(
            values = uiState.expenseDailyTotals,
            highlightDay = uiState.maxExpenseDay
        )

        Spacer(modifier = Modifier.height(8.dp))
        TrendAxisLabels(dayCount = uiState.expenseDailyTotals.size)
    }
}

@Composable
private fun TrendMetric(
    title: String,
    value: String,
    hint: String
) {
    Column(
        modifier = Modifier.width(94.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        if (hint.isNotBlank()) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpenseTrendChart(
    values: List<Double>,
    highlightDay: Int?
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
    ) {
        if (values.isEmpty()) {
            return@Canvas
        }

        val maxValue = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val width = size.width
        val height = size.height
        val stepX = if (values.size > 1) width / (values.size - 1) else width

        repeat(3) { index ->
            val y = height * (index + 1) / 4f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val points = values.mapIndexed { index, value ->
            val ratio = (value / maxValue).toFloat().coerceIn(0f, 1f)
            Offset(
                x = index * stepX,
                y = height - (ratio * height)
            )
        }

        val areaPath = Path().apply {
            moveTo(points.first().x, height)
            points.forEach { point ->
                lineTo(point.x, point.y)
            }
            lineTo(points.last().x, height)
            close()
        }
        drawPath(
            path = areaPath,
            color = lineColor.copy(alpha = 0.2f)
        )

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
        }
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        if (highlightDay != null && highlightDay in 1..points.size) {
            val highlightPoint = points[highlightDay - 1]
            drawCircle(
                color = lineColor,
                radius = 4.5.dp.toPx(),
                center = highlightPoint
            )
            drawCircle(
                color = surfaceColor,
                radius = 2.5.dp.toPx(),
                center = highlightPoint
            )
        }
    }
}

@Composable
private fun TrendAxisLabels(dayCount: Int) {
    val labels = listOf(1, 5, 10, 15, 20, 25, dayCount)
        .filter { it in 1..dayCount }
        .distinct()
        .sorted()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEach { day ->
            Text(
                text = day.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpenseCompareCard(uiState: MonthBillDetailUiState) {
    DetailCard {
        Text(
            text = "月支出对比",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))
        MonthBarChart(items = uiState.expenseCompareBars)

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${uiState.yearMonth.monthValue}月对比${uiState.yearMonth.minusMonths(1).monthValue}月变化前三的类别",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.topChangedExpenseCategories.isEmpty()) {
            Text(
                text = "暂无类别变化数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.topChangedExpenseCategories.forEachIndexed { index, item ->
                    CategoryChangeRow(
                        rank = index + 1,
                        item = item
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChangeRow(
    rank: Int,
    item: MonthCategoryChangeItem
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            modifier = Modifier.width(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            CategoryIconSymbol(
                name = item.categoryName,
                type = TransactionType.EXPENSE,
                iconKey = null,
                modifier = Modifier.size(17.dp)
            )
        }

        Text(
            text = item.categoryName,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        val iconColor = if (item.isIncrease) {
            MaterialTheme.colorScheme.error
        } else {
            androidx.compose.ui.graphics.Color(0xFF2E7D32)
        }
        Icon(
            imageVector = if (item.isIncrease) {
                Icons.Default.KeyboardArrowUp
            } else {
                Icons.Default.KeyboardArrowDown
            },
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = if (item.isIncrease) {
                "增长 ${formatAmount(abs(item.delta))}"
            } else {
                "下降 ${formatAmount(abs(item.delta))}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun IncomeCard(uiState: MonthBillDetailUiState) {
    DetailCard {
        Text(
            text = "收入",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "总收入 ${formatAmount(uiState.currentSummary.incomeTotal)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.incomeCategories.isEmpty()) {
            Text(
                text = "暂无收入数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.incomeCategories.forEach { item ->
                    IncomeCategoryRow(item = item)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "月收入对比",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        MonthBarChart(items = uiState.incomeCompareBars)
    }
}

@Composable
private fun IncomeCategoryRow(item: MonthCategoryRatioItem) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                CategoryIconSymbol(
                    name = item.categoryName,
                    type = item.type,
                    iconKey = null,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = "${item.categoryName} ${formatPercent(item.ratio)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            Text(
                text = formatAmount(item.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { item.ratio.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round,
            gapSize = 1.dp
        )
    }
}

@Composable
private fun MonthBarChart(items: List<MonthCompareBarItem>) {
    val maxAmount = items.maxOfOrNull { it.amount }?.takeIf { it > 0.0 } ?: 1.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val ratio = (item.amount / maxAmount).toFloat().coerceIn(0f, 1f)
            val rawHeight = (ratio * 102f).dp
            val minHeight = if (item.amount > 0.0) 8.dp else 0.dp
            val barHeight = if (rawHeight < minHeight) minHeight else rawHeight

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = formatCompactAmount(item.amount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(108.dp)
                        .fillMaxWidth(0.72f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (item.isCurrentMonth) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                }
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${item.month.monthValue}月",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (item.isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                    color = if (item.isCurrentMonth) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content
        )
    }
}

@Composable
private fun piePalette(): List<androidx.compose.ui.graphics.Color> {
    val scheme = MaterialTheme.colorScheme
    return listOf(
        scheme.primary,
        scheme.secondary,
        scheme.tertiary,
        scheme.error,
        scheme.primaryContainer,
        scheme.secondaryContainer,
        scheme.tertiaryContainer
    )
}

private fun formatPercent(ratio: Double): String {
    return String.format(Locale.getDefault(), "%.1f%%", ratio * 100)
}

private fun formatCompactAmount(amount: Double): String {
    val absAmount = abs(amount)
    return if (absAmount >= 10_000) {
        String.format(Locale.getDefault(), "%.1f万", amount / 10_000)
    } else {
        amount.toInt().toString()
    }
}

private fun YearMonth.toMonthLabel(): String {
    return "${monthValue}月"
}
