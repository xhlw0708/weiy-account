package com.weiy.account.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.weiy.account.BuildConfig
import com.weiy.account.R
import com.weiy.account.model.DataTransferFormat
import com.weiy.account.model.StartDestination
import com.weiy.account.model.TransactionType
import com.weiy.account.reminder.AccountingReminderScheduler
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
    onOpenRecurringAccounting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.uiState.collectAsState()
    val transferState by viewModel.dataTransferState.collectAsState()
    val context = LocalContext.current
    val reminderScheduler = remember(context) { AccountingReminderScheduler(context) }

    var pendingAction by remember { mutableStateOf<DataTransferAction?>(null) }
    var optionDialog by remember { mutableStateOf<SettingsOptionDialog?>(null) }
    var reminderDialogVisible by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableIntStateOf(settings.reminderHour) }
    var reminderMinute by remember { mutableIntStateOf(settings.reminderMinute) }
    var pendingReminderSave by remember { mutableStateOf(false) }
    var clearCacheDialogVisible by remember { mutableStateOf(false) }
    var cacheSizeLabel by remember { mutableStateOf(formatBytesSize(calculateAppCacheSizeBytes(context))) }

    LaunchedEffect(clearCacheDialogVisible) {
        if (!clearCacheDialogVisible) {
            cacheSizeLabel = formatBytesSize(calculateAppCacheSizeBytes(context))
        }
    }

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
    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                "未授予通知权限，提醒通知可能无法显示",
                Toast.LENGTH_SHORT
            ).show()
        }
        if (pendingReminderSave) {
            pendingReminderSave = false
            saveDailyReminder(
                context = context,
                viewModel = viewModel,
                reminderScheduler = reminderScheduler,
                hour = reminderHour,
                minute = reminderMinute
            )
            reminderDialogVisible = false
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

    if (reminderDialogVisible) {
        ReminderSettingsDialog(
            reminderEnabled = settings.reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            canScheduleExactReminders = reminderScheduler.canScheduleExactReminders(),
            onDismiss = {
                pendingReminderSave = false
                reminderDialogVisible = false
            },
            onPickTime = {
                openReminderTimePicker(
                    context = context,
                    initialHour = reminderHour,
                    initialMinute = reminderMinute
                ) { hour, minute ->
                    reminderHour = hour
                    reminderMinute = minute
                }
            },
            onOpenExactAlarmSettings = {
                exactAlarmSettingsLauncher.launch(reminderScheduler.createExactAlarmSettingsIntent())
            },
            onDisableReminder = {
                pendingReminderSave = false
                disableDailyReminder(
                    context = context,
                    viewModel = viewModel,
                    reminderScheduler = reminderScheduler
                )
                reminderDialogVisible = false
            },
            onSaveReminder = {
                if (hasNotificationPermission(context)) {
                    saveDailyReminder(
                        context = context,
                        viewModel = viewModel,
                        reminderScheduler = reminderScheduler,
                        hour = reminderHour,
                        minute = reminderMinute
                    )
                    reminderDialogVisible = false
                } else {
                    pendingReminderSave = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    }

    if (clearCacheDialogVisible) {
        AlertDialog(
            onDismissRequest = { clearCacheDialogVisible = false },
            title = { Text("清除缓存") },
            text = {
                Text(
                    text = "缓存是使用记账过程中产生的临时数据，清理缓存不会影响唯忆记账的正常使用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearCacheDialogVisible = false
                        if (clearAppCache(context)) {
                            cacheSizeLabel = formatBytesSize(calculateAppCacheSizeBytes(context))
                        }
                    }
                ) {
                    Text("清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { clearCacheDialogVisible = false }) {
                    Text("取消")
                }
            }
        )
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
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingsSectionTitle(
                title = "功能设置",
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
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Default.DateRange,
                    title = "日历设置",
                    subtitle = "开启后将在首页显示日历功能入口",
                    checked = settings.calendarEntryEnabled,
                    onCheckedChange = viewModel::setCalendarEntryEnabled
                )
            }
        }

        item {
            SettingsSectionTitle(
                title = "个性化设置",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            SettingsGroupCard {
                SettingsNavigationRow(
                    icon = ImageVector.vectorResource(R.drawable.ic_alarm),
                    title = "定时提醒",
                    subtitle = settings.reminderStatusLabel(),
                    enabled = true,
                    onClick = {
                        reminderHour = settings.reminderHour
                        reminderMinute = settings.reminderMinute
                        reminderDialogVisible = true
                    }
                )
            }
            SettingsDivider()
            SettingsGroupCard {
                SettingsNavigationRow(
                    icon = ImageVector.vectorResource(R.drawable.ic_timer),
                    title = "定时记账",
                    subtitle = "定时自动记账",
                    enabled = true,
                    onClick = onOpenRecurringAccounting
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

        item {
            SettingsSectionTitle(
                title = "系统设置",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            SettingsGroupCard {
                SettingsNavigationRow(
                    icon = ImageVector.vectorResource(R.drawable.ic_clean_cache),
                    title = "清除缓存",
                    subtitle = cacheSizeLabel,
                    enabled = true,
                    onClick = { clearCacheDialogVisible = true }
                )
            }
        }
    }
}

@Composable
private fun ReminderSettingsDialog(
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    canScheduleExactReminders: Boolean,
    onDismiss: () -> Unit,
    onPickTime: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onDisableReminder: () -> Unit,
    onSaveReminder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("定时提醒") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (reminderEnabled) {
                        "当前已开启每天 ${formatReminderTime(reminderHour, reminderMinute)} 的提醒"
                    } else {
                        "当前未开启提醒，保存后将每天按所选时间提醒记账"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onPickTime)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "提醒时间",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = formatReminderTime(reminderHour, reminderMinute),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (!canScheduleExactReminders) {
                    Text(
                        text = "系统未开启精确定时权限，提醒可能会有延迟。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onOpenExactAlarmSettings,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("开启准时提醒")
                    }
                }
                TextButton(
                    onClick = onDisableReminder,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("关闭提醒")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSaveReminder) {
                Text("保存提醒")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingsGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
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
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 58.dp),
        color = MaterialTheme.colorScheme.outlineVariant
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
        MaterialTheme.colorScheme.primary
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

private fun saveDailyReminder(
    context: Context,
    viewModel: SettingsViewModel,
    reminderScheduler: AccountingReminderScheduler,
    hour: Int,
    minute: Int
) {
    Log.d(
        AccountingReminderScheduler.LOG_TAG,
        "saveDailyReminder(): persist and schedule time=%02d:%02d".format(hour, minute)
    )
    viewModel.updateDailyReminder(hour, minute)
    reminderScheduler.cancelReminder()
    reminderScheduler.scheduleNextReminder(hour, minute)
    val message = if (reminderScheduler.canScheduleExactReminders()) {
        "已开启每天 ${formatReminderTime(hour, minute)} 的记账提醒"
    } else {
        "已保存提醒，系统可能不会准点触发"
    }
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun disableDailyReminder(
    context: Context,
    viewModel: SettingsViewModel,
    reminderScheduler: AccountingReminderScheduler
) {
    Log.d(AccountingReminderScheduler.LOG_TAG, "disableDailyReminder()")
    viewModel.disableDailyReminder()
    reminderScheduler.cancelReminder()
    Toast.makeText(context, "已关闭记账提醒", Toast.LENGTH_SHORT).show()
}

private fun clearAppCache(context: Context): Boolean {
    return runCatching {
        context.cacheDir.deleteRecursively()
        context.cacheDir.mkdirs()
        context.externalCacheDir?.let {
            it.deleteRecursively()
            it.mkdirs()
        }
    }.onSuccess {
        Toast.makeText(context, "缓存已清理", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "清理失败，请稍后重试", Toast.LENGTH_SHORT).show()
    }.isSuccess
}

private fun calculateAppCacheSizeBytes(context: Context): Long {
    val internal = context.cacheDir.directorySizeBytes()
    val external = context.externalCacheDir?.directorySizeBytes() ?: 0L
    return internal + external
}

private fun java.io.File.directorySizeBytes(): Long {
    if (!exists()) return 0L
    if (isFile) return length()
    return listFiles()?.sumOf { it.directorySizeBytes() } ?: 0L
}

private fun formatBytesSize(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index += 1
    }
    val display = if (index == 0) {
        value.toLong().toString()
    } else {
        String.format("%.2f", value)
    }
    return "缓存大小：$display ${units[index]}"
}

private fun hasNotificationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun openReminderTimePicker(
    context: Context,
    initialHour: Int,
    initialMinute: Int,
    onSelected: (Int, Int) -> Unit
) {
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onSelected(hourOfDay, minute)
        },
        initialHour,
        initialMinute,
        true
    ).show()
}

private fun formatReminderTime(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}

private fun com.weiy.account.model.AppSettings.reminderStatusLabel(): String {
    return if (reminderEnabled) {
        "每天 ${formatReminderTime(reminderHour, reminderMinute)} 提醒记账"
    } else {
        "未开启"
    }
}
