package com.weiy.account.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weiy.account.BuildConfig
import com.weiy.account.R
import com.weiy.account.model.DataTransferFormat
import com.weiy.account.model.StartDestination
import com.weiy.account.model.TransactionType
import com.weiy.account.viewmodel.DataTransferUiState
import com.weiy.account.viewmodel.SettingsViewModel

private enum class DataTransferAction(val title: String) {
    IMPORT("导入数据"),
    EXPORT("导出数据")
}

private enum class SettingsOptionDialog {
    START_DESTINATION,
    TRANSACTION_TYPE
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenCategoryManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.uiState.collectAsState()
    val transferState by viewModel.dataTransferState.collectAsState()
    var pendingAction by remember { mutableStateOf<DataTransferAction?>(null) }
    var optionDialog by remember { mutableStateOf<SettingsOptionDialog?>(null) }

    val importExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importData(uri = uri, format = DataTransferFormat.EXCEL)
        }
    }
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importData(uri = uri, format = DataTransferFormat.CSV)
        }
    }
    val exportExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(DataTransferFormat.EXCEL.mimeType)
    ) { uri ->
        if (uri != null) {
            viewModel.exportData(uri = uri, format = DataTransferFormat.EXCEL)
        }
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(DataTransferFormat.CSV.mimeType)
    ) { uri ->
        if (uri != null) {
            viewModel.exportData(uri = uri, format = DataTransferFormat.CSV)
        }
    }

    optionDialog?.let { dialog ->
        when (dialog) {
            SettingsOptionDialog.START_DESTINATION -> {
                SettingsChoiceDialog(
                    title = "默认启动页",
                    options = StartDestination.entries,
                    selected = settings.defaultStartDestination,
                    label = { it.displayName },
                    onDismiss = { optionDialog = null },
                    onSelected = {
                        viewModel.setDefaultStartDestination(it)
                        optionDialog = null
                    }
                )
            }

            SettingsOptionDialog.TRANSACTION_TYPE -> {
                SettingsChoiceDialog(
                    title = "默认记账类型",
                    options = TransactionType.entries,
                    selected = settings.defaultTransactionType,
                    label = { it.displayName },
                    onDismiss = { optionDialog = null },
                    onSelected = {
                        viewModel.setDefaultTransactionType(it)
                        optionDialog = null
                    }
                )
            }
        }
    }

    pendingAction?.let { action ->
        DataTransferFormatDialog(
            title = action.title,
            onDismiss = { pendingAction = null },
            onFormatSelected = { format ->
                pendingAction = null
                when (action) {
                    DataTransferAction.IMPORT -> {
                        when (format) {
                            DataTransferFormat.EXCEL -> {
                                importExcelLauncher.launch(
                                    arrayOf(DataTransferFormat.EXCEL.mimeType, "*/*")
                                )
                            }

                            DataTransferFormat.CSV -> {
                                importCsvLauncher.launch(
                                    arrayOf(DataTransferFormat.CSV.mimeType, "*/*")
                                )
                            }
                        }
                    }

                    DataTransferAction.EXPORT -> {
                        when (format) {
                            DataTransferFormat.EXCEL -> {
                                exportExcelLauncher.launch(format.buildSuggestedFileName())
                            }

                            DataTransferFormat.CSV -> {
                                exportCsvLauncher.launch(format.buildSuggestedFileName())
                            }
                        }
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7)),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingsSectionTitle(
                title = "基础设置",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            )
        }

        item {
            SettingsGroupCard {
                SettingsNavigationRow(
                    icon = Icons.Default.Home,
                    title = "默认启动页",
                    subtitle = "当前：${settings.defaultStartDestination.displayName}",
                    onClick = { optionDialog = SettingsOptionDialog.START_DESTINATION }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "默认记账类型",
                    subtitle = "当前：${settings.defaultTransactionType.displayName}",
                    onClick = { optionDialog = SettingsOptionDialog.TRANSACTION_TYPE }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Default.Menu,
                    title = "分类管理",
                    subtitle = "管理收入和支出的分类",
                    onClick = onOpenCategoryManage
                )
            }
        }

        item {
            SettingsSectionTitle(
                title = "界面与引导",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            SettingsGroupCard {
                if (BuildConfig.FLAVOR == "dev") {
                    SettingsSwitchRow(
                        icon = Icons.Default.PlayArrow,
                        title = "已完成新手引导",
                        subtitle = "关闭后，下次启动会重新进入引导流程",
                        checked = settings.onboardingShown,
                        onCheckedChange = viewModel::setOnboardingShown
                    )
                    SettingsDivider()
                }
                SettingsSwitchRow(
                    icon = Icons.Default.Settings,
                    title = "深色模式",
                    subtitle = "切换应用整体主题外观",
                    checked = settings.darkModeEnabled,
                    onCheckedChange = viewModel::setDarkModeEnabled
                )
            }
        }

        item {
            SettingsSectionTitle(
                title = "数据安全",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            SettingsGroupCard {
                SettingsNavigationRow(
                    icon = ImageVector.vectorResource(R.drawable.ic_import),
                    title = "导入数据",
                    subtitle = "导入分类、流水和备注历史",
                    enabled = !transferState.inProgress,
                    onClick = { pendingAction = DataTransferAction.IMPORT }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = ImageVector.vectorResource(R.drawable.ic_export),
                    title = "导出数据",
                    subtitle = "导出数据库中的全部数据",
                    enabled = !transferState.inProgress,
                    onClick = { pendingAction = DataTransferAction.EXPORT }
                )
            }
        }

        if (transferState.inProgress || transferState.message != null) {
            item {
                DataTransferStatusPanel(
                    state = transferState,
                    onClear = viewModel::clearDataTransferMessage,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFF7B7B80)
    )
}

@Composable
private fun SettingsGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsIcon(icon = icon)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsIcon(icon = icon)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(
                color = Color(0xFFF1F1F4),
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4E4E52)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 58.dp),
        color = Color(0xFFEAEAF0)
    )
}

@Composable
private fun DataTransferStatusPanel(
    state: DataTransferUiState,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (state.isError) {
        MaterialTheme.colorScheme.error
    } else {
        Color(0xFFE2B400)
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.inProgress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.4.dp,
                        color = accentColor
                    )
                    Text(
                        text = state.message ?: "正在处理数据…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = state.message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isError) accentColor else MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("清除提示")
                }
            }
        }
    }
}

@Composable
private fun <T> SettingsChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelected: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(option) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label(option),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (option == selected) {
                                Text(
                                    text = "当前",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun DataTransferFormatDialog(
    title: String,
    onDismiss: () -> Unit,
    onFormatSelected: (DataTransferFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DataTransferFormat.entries.forEach { format ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFormatSelected(format) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = if (format == DataTransferFormat.EXCEL) {
                                    painterResource(R.drawable.ic_excel)
                                } else {
                                    painterResource(R.drawable.ic_csv)
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = format.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = format.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
