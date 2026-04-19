package com.weiy.account.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.weiy.account.model.TransactionDateGroup
import com.weiy.account.ui.components.LongPressActionPopup
import com.weiy.account.ui.components.TopAnchorCenterPopupPositionProvider
import com.weiy.account.ui.components.TransactionListItem
import com.weiy.account.viewmodel.SearchUiState
import com.weiy.account.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        SearchInputBar(
            keyword = uiState.keywordInput,
            focusRequester = focusRequester,
            onKeywordChange = viewModel::onKeywordChange,
            onSearch = {
                viewModel.search()
                keyboardController?.hide()
            },
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.histories.isNotEmpty()) {
                item(key = "history_section") {
                    SearchHistorySection(
                        histories = uiState.histories,
                        onSearch = viewModel::searchByHistory,
                        onDelete = viewModel::removeHistory,
                        onClearAll = viewModel::clearHistories
                    )
                }
            }

            SearchResultSection(
                uiState = uiState,
                onOpenTransaction = onOpenTransaction
            )
        }
    }
}

@Composable
private fun SearchInputBar(
    keyword: String,
    focusRequester: FocusRequester,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回"
            )
        }

        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text("输入关键词搜索明细") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )

        TextButton(onClick = onSearch) {
            Text("搜索")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchHistorySection(
    histories: List<String>,
    onSearch: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    var longPressedKeyword by remember(histories) { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val popupPositionProvider = remember(density) {
        TopAnchorCenterPopupPositionProvider(
            verticalSpacing = with(density) { 8.dp.roundToPx() }
        )
    }

    val isDarkMenuTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val menuBackgroundColor = if (isDarkMenuTheme) Color.Black else Color.White
    val menuPrimaryTextColor = if (isDarkMenuTheme) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "历史搜索",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = {
                    longPressedKeyword = null
                    onClearAll()
                }
            ) {
                Text("清空")
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(histories, key = { it }) { historyKeyword ->
                val isLongPressed = longPressedKeyword == historyKeyword

                Column {
                    Surface(
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                longPressedKeyword = null
                                onSearch(historyKeyword)
                            },
                            onLongClick = {
                                longPressedKeyword =
                                    if (isLongPressed) null else historyKeyword
                            }
                        ),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = historyKeyword,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (isLongPressed) {
                        LongPressActionPopup(
                            popupPositionProvider = popupPositionProvider,
                            onDismissRequest = { longPressedKeyword = null },
                            shape = MaterialTheme.shapes.medium,
                            containerColor = menuBackgroundColor
                        ) {
                            TextButton(
                                onClick = {
                                    onDelete(historyKeyword)
                                    longPressedKeyword = null
                                },
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "删除",
                                    color = menuPrimaryTextColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.SearchResultSection(
    uiState: SearchUiState,
    onOpenTransaction: (Long) -> Unit
) {
    if (uiState.submittedKeyword.isBlank()) {
        item(key = "search_idle_hint") {
            Text(
                text = "输入关键词后点击键盘搜索即可查询",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    if (uiState.groupedResults.isEmpty()) {
        item(key = "search_empty_hint") {
            Text(
                text = "没有匹配到明细记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    uiState.groupedResults.forEach { monthGroup: TransactionDateGroup ->
        item(key = "search_month_${monthGroup.dateLabel}") {
            Text(
                text = monthGroup.dateLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(monthGroup.items, key = { transaction -> transaction.id }) { transaction ->
            TransactionListItem(
                transaction = transaction,
                onClick = onOpenTransaction
            )
        }
    }
}
