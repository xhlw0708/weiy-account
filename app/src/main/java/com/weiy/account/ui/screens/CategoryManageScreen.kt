package com.weiy.account.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.weiy.account.model.CategoryItem
import com.weiy.account.model.TransactionType
import com.weiy.account.viewmodel.CategoryManageViewModel

private val CategoryTextColor = Color(0xFF3E3E3E)
private val CategoryHintColor = Color(0xFF8A8A8A)
private val CategoryIndicatorColor = Color(0xFF1F1F1F)
private val CategoryDangerColor = Color(0xFFD95C54)

@Composable
fun CategoryManageScreen(
    viewModel: CategoryManageViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var messageText by remember { mutableStateOf<String?>(null) }

    if (showAddDialog) {
        CategoryNameDialog(
            title = "新增分类",
            confirmText = "添加",
            initialValue = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.addCategory(name)
                showAddDialog = false
            }
        )
    }

    editingCategory?.let { category ->
        CategoryNameDialog(
            title = "编辑分类",
            confirmText = "保存",
            initialValue = category.name,
            onDismiss = { editingCategory = null },
            onConfirm = { name ->
                viewModel.renameCategory(category.id, name)
                editingCategory = null
            }
        )
    }

    deletingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("删除分类") },
            text = { Text("确认删除“${category.name}”吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category) { errorMessage ->
                            messageText = errorMessage
                        }
                        deletingCategory = null
                    }
                ) {
                    Text("删除", color = CategoryDangerColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text("取消")
                }
            }
        )
    }

    messageText?.let { message ->
        AlertDialog(
            onDismissRequest = { messageText = null },
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { messageText = null }) {
                    Text("我知道了")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        CategoryManageHeader(
            selectedType = uiState.selectedType,
            onTypeSelected = viewModel::selectType,
            onCancel = onBack
        )
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                if (uiState.categories.isEmpty()) {
                    item(
                        key = "empty_hint",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Text(
                            text = "还没有分类，先添加一个吧",
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = CategoryHintColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                items(uiState.categories, key = { it.id }) { category ->
                    CategoryGridItem(
                        category = category,
                        onClick = { editingCategory = category },
                        onEdit = { editingCategory = category },
                        onDelete = { deletingCategory = category }
                    )
                }

                item(key = "add_category") {
                    AddCategoryGridItem(onClick = { showAddDialog = true })
                }
            }
        }
    }
}

@Composable
private fun CategoryManageHeader(
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
            Box(modifier = Modifier.size(width = 54.dp, height = 36.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                CategoryTypeTab(
                    label = "支出",
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { onTypeSelected(TransactionType.EXPENSE) }
                )
                CategoryTypeTab(
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
                    color = CategoryTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CategoryTypeTab(
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
            color = CategoryTextColor,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(width = 28.dp, height = 3.dp)
                .clip(CircleShape)
                .background(if (selected) CategoryIndicatorColor else Color.Transparent)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryGridItem(
    category: CategoryItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember(category.id) { mutableStateOf(false) }
    val density = LocalDensity.current
    val isDarkMenuTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val menuBackgroundColor = if (isDarkMenuTheme) Color.Black else Color.White
    val menuPrimaryTextColor = if (isDarkMenuTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val menuDisabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val menuDeleteColor = if (category.isDefault) menuDisabledTextColor else MaterialTheme.colorScheme.error
    val menuDividerColor = if (isDarkMenuTheme) {
        Color.White.copy(alpha = 0.24f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    }
    val popupPositionProvider = remember(density) {
        CategoryActionMenuPositionProvider(
            verticalSpacing = with(density) { 8.dp.roundToPx() }
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(modifier = Modifier.wrapContentSize()) {
            Column(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { menuExpanded = true }
                    )
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
                    color = CategoryTextColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            if (menuExpanded) {
                Popup(
                    popupPositionProvider = popupPositionProvider,
                    onDismissRequest = { menuExpanded = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = menuBackgroundColor,
                        shadowElevation = 10.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryActionMenuItem(
                                label = "编辑",
                                color = menuPrimaryTextColor,
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            VerticalMenuDivider(color = menuDividerColor)
                            CategoryActionMenuItem(
                                label = "删除",
                                color = menuDeleteColor,
                                enabled = !category.isDefault,
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryActionMenuItem(
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(40.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun VerticalMenuDivider(color: Color) {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 18.dp)
            .background(color)
    )
}

@Composable
private fun AddCategoryGridItem(
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
            AddCategoryIcon(modifier = Modifier.size(24.dp))
        }
        Text(
            text = "新增",
            modifier = Modifier.padding(top = 8.dp),
            color = CategoryTextColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun CategoryNameDialog(
    title: String,
    confirmText: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                label = { Text("分类名称") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(input.trim()) },
                enabled = input.isNotBlank()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private class CategoryActionMenuPositionProvider(
    private val verticalSpacing: Int
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val centeredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val clampedX = centeredX.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))

        val preferredY = anchorBounds.top - popupContentSize.height - verticalSpacing
        val fallbackY = anchorBounds.bottom + verticalSpacing
        val finalY = if (preferredY >= 0) {
            preferredY
        } else {
            fallbackY.coerceAtMost(windowSize.height - popupContentSize.height)
        }.coerceAtLeast(0)

        return IntOffset(clampedX, finalY)
    }
}
