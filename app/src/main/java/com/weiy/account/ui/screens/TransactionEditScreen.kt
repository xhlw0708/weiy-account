package com.weiy.account.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.weiy.account.model.TransactionType
import com.weiy.account.utils.formatDateTime
import com.weiy.account.viewmodel.TransactionEditEvent
import com.weiy.account.viewmodel.TransactionEditViewModel
import java.util.Calendar

@Composable
fun TransactionEditScreen(
    viewModel: TransactionEditViewModel,
    onFinished: () -> Unit,
    onOpenCategoryManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var categoryExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                TransactionEditEvent.Saved -> onFinished()
                TransactionEditEvent.Deleted -> onFinished()
            }
        }
    }

    val selectedCategoryName = uiState.categories.firstOrNull { it.id == uiState.categoryId }?.name.orEmpty()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (uiState.isEditMode) "编辑账目" else "新增账目",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.onTypeChange(TransactionType.EXPENSE) },
                    label = { Text("支出") }
                )
                FilterChip(
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.onTypeChange(TransactionType.INCOME) },
                    label = { Text("收入") }
                )
            }
        }

        item {
            OutlinedTextField(
                value = uiState.amountInput,
                onValueChange = viewModel::onAmountChange,
                label = { Text("金额") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        }

        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分类") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { categoryExpanded = true }
                )
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    uiState.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                categoryExpanded = false
                                viewModel.onCategoryChange(category.id)
                            }
                        )
                    }
                }
            }
        }

        item {
            TextButton(
                onClick = {
                    val calendar = Calendar.getInstance().apply { timeInMillis = uiState.dateTime }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            calendar.set(Calendar.YEAR, year)
                            calendar.set(Calendar.MONTH, month)
                            calendar.set(Calendar.DAY_OF_MONTH, day)
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    calendar.set(Calendar.MINUTE, minute)
                                    calendar.set(Calendar.SECOND, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)
                                    viewModel.onDateTimeChange(calendar.timeInMillis)
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Text("日期时间：${formatDateTime(uiState.dateTime)}")
            }
        }

        item {
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            item {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Button(
                onClick = viewModel::saveTransaction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }

        if (uiState.canDelete) {
            item {
                Button(
                    onClick = viewModel::deleteTransaction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("删除")
                }
            }
        }

        item {
            TextButton(
                onClick = onOpenCategoryManage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("管理分类")
            }
        }
    }
}
