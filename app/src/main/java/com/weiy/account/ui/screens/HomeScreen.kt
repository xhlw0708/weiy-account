package com.weiy.account.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weiy.account.ui.components.TransactionListItem
import com.weiy.account.utils.formatAmount
import com.weiy.account.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onQuickAdd: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onFirstItemCompletelyInvisibleChanged: (Boolean) -> Unit = {},
    scrollToTopSignal: Int = 0,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val density = LocalDensity.current

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
                    ElevatedCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${uiState.monthLabel} 概览",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
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

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "最近记录",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = onQuickAdd) {
                            Text("快速记账")
                        }
                    }
                }

                if (uiState.groupedTransactions.isEmpty()) {
                    item {
                        ElevatedCard {
                            Text(
                                text = "暂无账目，点击右下方按钮开始记录吧",
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
}

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
