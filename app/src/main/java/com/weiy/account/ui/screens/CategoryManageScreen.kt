package com.weiy.account.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weiy.account.model.CategoryItem
import com.weiy.account.model.TransactionType
import com.weiy.account.viewmodel.CategoryManageViewModel

@Composable
fun CategoryManageScreen(
    viewModel: CategoryManageViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var editingName by remember { mutableStateOf("") }

    if (editingCategory != null) {
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("编辑分类") },
            text = {
                OutlinedTextField(
                    value = editingName,
                    onValueChange = { editingName = it },
                    label = { Text("分类名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val category = editingCategory ?: return@TextButton
                        viewModel.renameCategory(category.id, editingName)
                        editingCategory = null
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("取消")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = onBack) {
                Text("返回")
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.selectedType == TransactionType.EXPENSE,
                    onClick = { viewModel.selectType(TransactionType.EXPENSE) },
                    label = { Text("支出") }
                )
                FilterChip(
                    selected = uiState.selectedType == TransactionType.INCOME,
                    onClick = { viewModel.selectType(TransactionType.INCOME) },
                    label = { Text("收入") }
                )
            }
        }

        item {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text("新增分类") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Button(
                onClick = {
                    viewModel.addCategory(newCategoryName)
                    newCategoryName = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("添加分类")
            }
        }

        if (uiState.categories.isEmpty()) {
            item {
                Text(
                    text = "暂无分类",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.categories, key = { it.id }) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingCategory = category
                            editingName = category.name
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (category.isDefault) "默认" else "自定义",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
