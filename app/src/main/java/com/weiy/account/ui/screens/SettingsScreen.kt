package com.weiy.account.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weiy.account.model.StartDestination
import com.weiy.account.model.TransactionType
import com.weiy.account.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenCategoryManage: () -> Unit,
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
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("默认启动页", style = MaterialTheme.typography.titleMedium)
                    DestinationSelectRow(
                        selected = uiState.defaultStartDestination,
                        onSelected = viewModel::setDefaultStartDestination
                    )
                }
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("默认记账类型", style = MaterialTheme.typography.titleMedium)
                    TypeSelectRow(
                        selected = uiState.defaultTransactionType,
                        onSelected = viewModel::setDefaultTransactionType
                    )
                }
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingSwitchRow(
                        title = "已显示新手引导",
                        checked = uiState.onboardingShown,
                        onCheckedChange = viewModel::setOnboardingShown
                    )
                    SettingSwitchRow(
                        title = "启用深色模式",
                        checked = uiState.darkModeEnabled,
                        onCheckedChange = viewModel::setDarkModeEnabled
                    )
                }
            }
        }

        item {
            Button(
                onClick = onOpenCategoryManage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("管理分类")
            }
        }
    }
}

@Composable
private fun DestinationSelectRow(
    selected: StartDestination,
    onSelected: (StartDestination) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StartDestination.entries.forEach { destination ->
            FilterChip(
                selected = destination == selected,
                onClick = { onSelected(destination) },
                label = { Text(destination.displayName) }
            )
        }
    }
}

@Composable
private fun TypeSelectRow(
    selected: TransactionType,
    onSelected: (TransactionType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TransactionType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text(type.displayName) }
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
