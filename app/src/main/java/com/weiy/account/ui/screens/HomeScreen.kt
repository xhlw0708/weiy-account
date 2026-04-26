package com.weiy.account.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.weiy.account.R
import com.weiy.account.ui.components.WeiyCalendar
import com.weiy.account.ui.components.WeiyCalendarView
import java.time.LocalDate
import com.weiy.account.ui.components.TransactionListItem
import com.weiy.account.utils.currentYearMonth
import com.weiy.account.utils.formatAmount
import com.weiy.account.viewmodel.HomeViewModel
import java.time.YearMonth
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    calendarEntryEnabled: Boolean,
    onOpenTransaction: (Long) -> Unit,
    onFirstItemCompletelyInvisibleChanged: (Boolean) -> Unit = {},
    scrollToTopSignal: Int = 0,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var showMonthPicker by remember { mutableStateOf(false) }
    var isCardFlipped by remember { mutableStateOf(false) }
    val flipRotation by animateFloatAsState(
        targetValue = if (isCardFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500)
    )
    var calendarVisibleMonth by remember { mutableStateOf(uiState.currentMonth) }
    var calendarSelectedDate by remember { mutableStateOf(LocalDate.now()) }
    var calendarView by remember { mutableStateOf(WeiyCalendarView.Month) }

    LaunchedEffect(uiState.currentMonth) {
        if (!isCardFlipped) {
            calendarVisibleMonth = uiState.currentMonth
        }
    }

    val pullTriggerPx = with(density) { 96.dp.toPx() }
    val pullMaxPx = with(density) { 150.dp.toPx() }
    var pullUpDistancePx by remember { mutableFloatStateOf(0f) }
    var pullUpCanceledInCurrentGesture by remember { mutableStateOf(false) }
    var pullUpStartedInCurrentGesture by remember { mutableStateOf(false) }
    val isRefreshing by rememberUpdatedState(uiState.isRefreshing)
    val isLoadingPreviousMonth by rememberUpdatedState(uiState.isLoadingPreviousMonth)
    val isFirstItemCompletelyInvisible by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || isLoadingPreviousMonth || isRefreshing) {
                    return Offset.Zero
                }
                if (available.y > 0f && pullUpStartedInCurrentGesture) {
                    pullUpCanceledInCurrentGesture = true
                    pullUpDistancePx = 0f
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput || isLoadingPreviousMonth || isRefreshing) {
                    return Offset.Zero
                }
                if (available.y > 0f && pullUpDistancePx > 0f) {
                    pullUpCanceledInCurrentGesture = true
                    pullUpDistancePx = 0f
                    return Offset.Zero
                }

                if (!listState.canScrollForward && available.y < 0f && !pullUpCanceledInCurrentGesture) {
                    pullUpDistancePx = (pullUpDistancePx - available.y).coerceAtMost(pullMaxPx)
                    if (pullUpDistancePx > 0f) {
                        pullUpStartedInCurrentGesture = true
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (
                    !pullUpCanceledInCurrentGesture &&
                    !isLoadingPreviousMonth &&
                    !isRefreshing &&
                    pullUpDistancePx >= pullTriggerPx
                ) {
                    viewModel.loadPreviousMonthByPullUp()
                }
                pullUpDistancePx = 0f
                pullUpCanceledInCurrentGesture = false
                pullUpStartedInCurrentGesture = false
                return Velocity.Zero
            }
        }
    }

    val loadMoreHint = when {
        uiState.isRefreshing -> "加载中..."
        uiState.isLoadingPreviousMonth -> "加载中..."
        pullUpDistancePx >= pullTriggerPx -> "松开查看上月数据"
        else -> "上拉查看上月数据"
    }
    val showLoadMoreHint = uiState.isRefreshing || uiState.isLoadingPreviousMonth || pullUpDistancePx > 0f
    val loadMoreHintColor = if (pullUpDistancePx >= pullTriggerPx) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    LaunchedEffect(uiState.currentMonth) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(isFirstItemCompletelyInvisible) {
        onFirstItemCompletelyInvisibleChanged(isFirstItemCompletelyInvisible)
    }

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            listState.animateScrollToItem(0)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onFirstItemCompletelyInvisibleChanged(false)
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refreshOrLoadNextMonth,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = if (showLoadMoreHint) 56.dp else 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Front - Summary card
                        if (flipRotation < 90f) {
                            ElevatedCard(
                                modifier = Modifier.graphicsLayer {
                                    rotationY = flipRotation
                                    cameraDistance = 12f * density.density
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = uiState.monthLabel,
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Icon(
                                                modifier = Modifier.clickable { showMonthPicker = true },
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "选择时间",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (calendarEntryEnabled) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_switch_over),
                                                contentDescription = "日历入口",
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .padding(end = 2.dp)
                                                    .clickable { isCardFlipped = true },
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        MetricItem(
                                            title = "本月支出",
                                            amount = formatAmount(uiState.summary.expenseTotal),
                                            amountColor = Color(0xFFD32F2F)
                                        )
                                        MetricItem(
                                            title = "本月收入",
                                            amount = formatAmount(uiState.summary.incomeTotal),
                                            amountColor = Color(0xFF2E7D32)
                                        )
                                        MetricItem(
                                            title = "结余",
                                            amount = formatAmount(uiState.summary.balance),
                                            amountColor = Color(0xFF1565C0)
                                        )
                                    }
                                    IncomeExpenseProgressBar(
                                        income = uiState.summary.incomeTotal,
                                        expense = uiState.summary.expenseTotal
                                    )
                                }
                            }
                        }
                        // Back - Calendar
                        if (flipRotation > 90f) {
                            ElevatedCard(
                                modifier = Modifier.graphicsLayer {
                                    rotationY = flipRotation + 180f
                                    cameraDistance = 12f * density.density
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "日历",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.ic_switch_over),
                                            contentDescription = "返回",
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { isCardFlipped = false },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    WeiyCalendar(
                                        entries = emptyList(),
                                        visibleMonth = calendarVisibleMonth,
                                        selectedDate = calendarSelectedDate,
                                        view = calendarView,
                                        onVisibleMonthChange = { calendarVisibleMonth = it },
                                        onSelectedDateChange = { calendarSelectedDate = it },
                                        onViewChange = { calendarView = it }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "最近记录",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "日历",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.groupedTransactions.isEmpty()) {
                    item {
                        ElevatedCard {
                            Text(
                                text = "暂无明细，点击右下方按钮开始记录吧",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    uiState.groupedTransactions.forEach { group ->
                        item(key = "home_header_${group.dateLabel}") {
                            Text(
                                text = group.dateLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        items(group.items, key = { it.id }) { transaction ->
                            TransactionListItem(
                                transaction = transaction,
                                incomeAmountColor = Color(0xFF2E7D32),
                                onClick = onOpenTransaction
                            )
                        }
                    }
                }
            }

            if (showLoadMoreHint) {
                Text(
                    text = loadMoreHint,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = loadMoreHintColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showMonthPicker) {
        HomeMonthPickerBottomSheet(
            currentMonth = uiState.currentMonth,
            onDismiss = { showMonthPicker = false },
            onConfirm = { year, month ->
                viewModel.selectMonth(year, month)
                showMonthPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeMonthPickerBottomSheet(
    currentMonth: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val latestMonth = currentYearMonth()
    val latestYear = latestMonth.year
    val latestMonthValue = latestMonth.monthValue

    var pendingYear by remember(currentMonth, latestYear) {
        mutableIntStateOf(currentMonth.year.coerceIn(MIN_SELECTABLE_YEAR, latestYear))
    }
    var pendingMonth by remember(currentMonth) {
        mutableIntStateOf(currentMonth.monthValue)
    }

    val yearOptions = remember(latestYear) {
        (MIN_SELECTABLE_YEAR..latestYear).toList()
    }
    val maxMonthForPendingYear = if (pendingYear == latestYear) latestMonthValue else 12
    val monthOptions = remember(maxMonthForPendingYear) {
        (1..maxMonthForPendingYear).toList()
    }

    LaunchedEffect(maxMonthForPendingYear) {
        pendingMonth = pendingMonth.coerceIn(1, maxMonthForPendingYear)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Text(
                    text = "选择月份",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = { onConfirm(pendingYear, pendingMonth) }) {
                    Text("确定")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(vertical = 20.dp, horizontal = 20.dp)
                    .heightIn(min = PICKER_VIEWPORT_HEIGHT, max = PICKER_VIEWPORT_HEIGHT + 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                WheelPickerColumn(
                    options = yearOptions,
                    selectedValue = pendingYear,
                    onValueSelected = { pendingYear = it },
                    modifier = Modifier.weight(1f)
                )
                WheelPickerColumn(
                    options = monthOptions,
                    selectedValue = pendingMonth,
                    onValueSelected = { pendingMonth = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WheelPickerColumn(
    options: List<Int>,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return

    val scope = rememberCoroutineScope()
    val itemHeightPx = with(LocalDensity.current) { PICKER_ITEM_HEIGHT.roundToPx() }
    val selectedIndex = options.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(options, selectedValue) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        val targetIndex = options.indexOf(selectedValue).coerceAtLeast(0)
        if (listState.firstVisibleItemIndex != targetIndex || listState.firstVisibleItemScrollOffset != 0) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState, options, itemHeightPx) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .map { (index, offset) ->
                (index + if (offset >= itemHeightPx / 2) 1 else 0).coerceIn(0, options.lastIndex)
            }
            .distinctUntilChanged()
            .collect { index ->
                onValueSelected(options[index])
            }
    }

    Box(
        modifier = modifier.height(PICKER_VIEWPORT_HEIGHT)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = PICKER_ITEM_HEIGHT)
        ) {
            itemsIndexed(options, key = { _, value -> value }) { index, value ->
                val isSelected = value == selectedValue
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PICKER_ITEM_HEIGHT)
                        .clickable {
                            scope.launch {
                                listState.animateScrollToItem(index)
                            }
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.toString(),
                        textAlign = TextAlign.Center,
                        style = if (isSelected) {
                            MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium)
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(
                modifier = Modifier.width(100.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
            )
            Spacer(modifier = Modifier.height(PICKER_ITEM_HEIGHT))
            HorizontalDivider(
                modifier = Modifier.width(100.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
            )
        }
    }
}

private const val MIN_SELECTABLE_YEAR = 2002
private val PICKER_ITEM_HEIGHT = 64.dp
private val PICKER_VIEWPORT_HEIGHT = 192.dp

@Composable
private fun MetricItem(
    title: String,
    amount: String,
    amountColor: Color
) {
    Column(
        modifier = Modifier.width(96.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            color = amountColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IncomeExpenseProgressBar(
    income: Double,
    expense: Double
) {
    val total = income + expense
    val incomeRatio = if (total > 0) (income / total).toFloat() else 0f
    val expenseRatio = if (total > 0) (expense / total).toFloat() else 0f

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(99.dp))
        ) {
            if (expenseRatio > 0f) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(expenseRatio)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.errorContainer)
                )
            }
            if (incomeRatio > 0f) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(incomeRatio)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "支出占比 ${"%.0f".format(expenseRatio * 100)}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "收入占比 ${"%.0f".format(incomeRatio * 100)}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

