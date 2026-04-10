package com.weiy.account.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weiy.account.model.CategoryItem
import com.weiy.account.model.TransactionType
import com.weiy.account.utils.formatDateTime
import com.weiy.account.viewmodel.TransactionEditEvent
import com.weiy.account.viewmodel.TransactionEditViewModel
import java.util.Calendar

private val TransactionEditTextColor = Color(0xFF3E3E3E)
private val TransactionEditHintColor = Color(0xFF8A8A8A)
private val TransactionEditIndicatorColor = Color(0xFF1F1F1F)
private val TransactionEditDangerColor = Color(0xFFD95C54)
private val TransactionEditPrimaryButtonColor = Color(0xFF1F1F1F)

@Composable
fun TransactionEditScreen(
    viewModel: TransactionEditViewModel,
    onFinished: () -> Unit,
    onOpenCategoryManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                TransactionEditEvent.Saved -> onFinished()
                TransactionEditEvent.Deleted -> onFinished()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        TransactionEditHeader(
            title = if (uiState.isEditMode) "编辑账目" else "新增账目",
            selectedType = uiState.type,
            onTypeSelected = viewModel::onTypeChange,
            onCancel = onFinished
        )
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TransactionAmountSection(
                    amountInput = uiState.amountInput,
                    onAmountChange = viewModel::onAmountChange
                )

                TransactionCategorySection(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.categoryId,
                    onCategoryClick = viewModel::onCategoryChange,
                    onOpenCategoryManage = onOpenCategoryManage
                )

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                TransactionInfoRow(
                    label = "日期时间",
                    value = formatDateTime(uiState.dateTime),
                    onClick = {
                        openDateTimePicker(
                            context = context,
                            initialValue = uiState.dateTime,
                            onSelected = viewModel::onDateTimeChange
                        )
                    }
                )

                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    minLines = 3
                )

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = viewModel::saveTransaction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TransactionEditPrimaryButtonColor
                    )
                ) {
                    Text(
                        text = if (uiState.isEditMode) "保存修改" else "保存账目",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (uiState.canDelete) {
                    TextButton(
                        onClick = viewModel::deleteTransaction,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "删除账目",
                            color = TransactionEditDangerColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCategorySection(
    categories: List<CategoryItem>,
    selectedCategoryId: Long?,
    onCategoryClick: (Long) -> Unit,
    onOpenCategoryManage: () -> Unit
) {
    val categoryGridState = remember(categories.firstOrNull()?.id, categories.size) {
        LazyGridState()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(text = "选择分类")

        if (categories.isEmpty()) {
            Text(
                text = "暂无分类，先去管理分类页添加一个吧",
                color = TransactionEditHintColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = categoryGridState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                TransactionCategoryGridItem(
                    category = category,
                    selected = category.id == selectedCategoryId,
                    onClick = { onCategoryClick(category.id) }
                )
            }

            item(key = "manage_category") {
                ManageCategoryGridItem(onClick = onOpenCategoryManage)
            }
        }
    }
}

@Composable
private fun TransactionEditHeader(
    title: String,
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TransactionEditTextColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(modifier = Modifier.size(width = 54.dp, height = 36.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                TransactionTypeTab(
                    label = "支出",
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { onTypeSelected(TransactionType.EXPENSE) }
                )
                TransactionTypeTab(
                    label = "收入",
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { onTypeSelected(TransactionType.INCOME) }
                )
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.size(width = 54.dp, height = 36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "取消",
                    color = TransactionEditTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TransactionTypeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = TransactionEditTextColor,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(width = 28.dp, height = 3.dp)
                .clip(CircleShape)
                .background(if (selected) TransactionEditIndicatorColor else Color.Transparent)
        )
    }
}

@Composable
private fun TransactionAmountSection(
    amountInput: String,
    onAmountChange: (String) -> Unit
) {
    OutlinedTextField(
        value = amountInput,
        onValueChange = onAmountChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("金额") },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            color = TransactionEditTextColor
        )
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TransactionEditTextColor,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
private fun TransactionCategoryGridItem(
    category: CategoryItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else CategoryManageItemCircleColor
                ),
            contentAlignment = Alignment.Center
        ) {
            CategoryIconSymbol(
                name = category.name,
                type = category.type,
                iconKey = category.iconKey,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = category.name,
            modifier = Modifier.padding(top = 8.dp),
            color = TransactionEditTextColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(width = 22.dp, height = 3.dp)
                .clip(CircleShape)
                .background(if (selected) TransactionEditIndicatorColor else Color.Transparent)
        )
    }
}

@Composable
private fun ManageCategoryGridItem(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(CategoryManageItemCircleColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "管",
                color = TransactionEditTextColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        Text(
            text = "管理",
            modifier = Modifier.padding(top = 8.dp),
            color = TransactionEditTextColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TransactionInfoRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = CategoryManageItemCircleColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TransactionEditTextColor,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = value,
                color = TransactionEditHintColor,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End
            )
        }
    }
}

private fun openDateTimePicker(
    context: android.content.Context,
    initialValue: Long,
    onSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialValue }

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
                    onSelected(calendar.timeInMillis)
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
