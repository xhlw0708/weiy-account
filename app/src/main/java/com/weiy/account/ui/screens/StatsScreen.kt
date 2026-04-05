package com.weiy.account.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weiy.account.ui.components.CategoryStatRow
import com.weiy.account.ui.components.MonthSelectorRow
import com.weiy.account.ui.components.SummaryInfoCard
import com.weiy.account.viewmodel.StatsViewModel

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
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
            MonthSelectorRow(
                monthLabel = uiState.monthLabel,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )
        }

        item { SummaryInfoCard(title = "本月支出", amount = uiState.summary.expenseTotal) }
        item { SummaryInfoCard(title = "本月收入", amount = uiState.summary.incomeTotal) }
        item { SummaryInfoCard(title = "本月结余", amount = uiState.summary.balance) }

        item {
            Card {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = "支出分类", style = MaterialTheme.typography.titleMedium)
                    if (uiState.expenseStats.isEmpty()) {
                        Text(
                            text = "暂无支出数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val total = uiState.expenseStats.sumOf { it.totalAmount }
                        uiState.expenseStats.forEach { item ->
                            CategoryStatRow(
                                categoryName = item.categoryName,
                                amount = item.totalAmount,
                                total = total
                            )
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = "收入分类", style = MaterialTheme.typography.titleMedium)
                    if (uiState.incomeStats.isEmpty()) {
                        Text(
                            text = "暂无收入数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val total = uiState.incomeStats.sumOf { it.totalAmount }
                        uiState.incomeStats.forEach { item ->
                            CategoryStatRow(
                                categoryName = item.categoryName,
                                amount = item.totalAmount,
                                total = total
                            )
                        }
                    }
                }
            }
        }
    }
}
