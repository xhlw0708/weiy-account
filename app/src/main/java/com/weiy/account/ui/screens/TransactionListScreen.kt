package com.weiy.account.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weiy.account.model.MonthBill
import com.weiy.account.model.YearBill
import com.weiy.account.utils.formatAmount
import com.weiy.account.viewmodel.BillTab
import com.weiy.account.viewmodel.TransactionListUiState
import com.weiy.account.viewmodel.TransactionListViewModel

@Suppress("UNUSED_PARAMETER")
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onAddTransaction: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showYearPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BillTopBar(
                uiState = uiState,
                onSelectTab = viewModel::selectTab,
                onOpenYearPicker = {
                    viewModel.refreshBillYearRange()
                    showYearPicker = true
                }
            )
        }

        item {
            BillSummaryCard(uiState = uiState)
        }

        item {
            BillTable(uiState = uiState)
        }

        if (uiState.showYearHint) {
            item {
                Text(
                    text = "年账单为自然年（1.1-12.31）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showYearPicker && uiState.tab == BillTab.MONTH) {
        YearPickerBottomSheet(
            selectedYear = uiState.selectedYear,
            minYear = uiState.minSelectableYear,
            maxYear = uiState.maxSelectableYear,
            onDismiss = { showYearPicker = false },
            onConfirm = { year ->
                viewModel.selectYear(year)
                showYearPicker = false
            }
        )
    }
}

@Composable
private fun BillTopBar(
    uiState: TransactionListUiState,
    onSelectTab: (BillTab) -> Unit,
    onOpenYearPicker: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (uiState.tab == BillTab.MONTH) {
            YearSwitcher(
                selectedYear = uiState.selectedYear,
                onClick = onOpenYearPicker
            )
        } else {
            Spacer(modifier = Modifier.width(108.dp))
        }

        BillTabSwitcher(
            tab = uiState.tab,
            onSelectTab = onSelectTab
        )
    }
}

@Composable
private fun YearSwitcher(
    selectedYear: Int,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick) {
        Text(text = "${selectedYear}年")
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "选择年份"
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun YearPickerBottomSheet(
    selectedYear: Int,
    minYear: Int,
    maxYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var pendingYear by remember(selectedYear) { mutableStateOf(selectedYear) }
    val yearOptions = remember(minYear, maxYear) {
        (maxYear downTo minYear).toList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Text(
                    text = "选择年份",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = { onConfirm(pendingYear) }) {
                    Text("确定")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                items(yearOptions) { year ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pendingYear = year
                                onConfirm(year)
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (year == pendingYear) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun BillTabSwitcher(
    tab: BillTab,
    onSelectTab: (BillTab) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .padding(2.dp)
        ) {
            BillTabButton(
                text = "月账单",
                selected = tab == BillTab.MONTH,
                onClick = { onSelectTab(BillTab.MONTH) }
            )
            BillTabButton(
                text = "年账单",
                selected = tab == BillTab.YEAR,
                onClick = { onSelectTab(BillTab.YEAR) }
            )
        }
    }
}

@Composable
private fun RowScope.BillTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun BillSummaryCard(uiState: TransactionListUiState) {
    val summary = uiState.summaryCard
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = formatAmount(summary.balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = summary.incomeLabel,
                    value = summary.incomeTotal,
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    label = summary.expenseLabel,
                    value = summary.expenseTotal,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = formatAmount(value),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun BillTable(uiState: TransactionListUiState) {
    val isMonth = uiState.tab == BillTab.MONTH

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BillTableHeader(isMonth = isMonth)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (isMonth) {
                if (uiState.monthBills.isEmpty()) {
                    EmptyRow(text = "${uiState.selectedYear}年暂无账单")
                } else {
                    uiState.monthBills.forEachIndexed { index, item ->
                        BillMonthRow(item = item)
                        if (index != uiState.monthBills.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            } else {
                if (uiState.yearBills.isEmpty()) {
                    EmptyRow(text = "暂无年账单")
                } else {
                    uiState.yearBills.forEachIndexed { index, item ->
                        BillYearRow(item = item)
                        if (index != uiState.yearBills.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillTableHeader(isMonth: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isMonth) "月份" else "年份",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (isMonth) "月收入" else "年收入",
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (isMonth) "月支出" else "年支出",
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (isMonth) "月结余" else "年结余",
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isMonth) {
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Composable
private fun BillMonthRow(item: MonthBill) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${item.month}月",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = formatAmount(item.incomeTotal),
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = formatAmount(item.expenseTotal),
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = formatAmount(item.balance),
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun BillYearRow(item: YearBill) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${item.year}年",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = formatAmount(item.incomeTotal),
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = formatAmount(item.expenseTotal),
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = formatAmount(item.balance),
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)
    )
}
