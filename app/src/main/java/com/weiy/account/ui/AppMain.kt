package com.weiy.account.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.weiy.account.data.AppContainer
import com.weiy.account.navigation.AppRoute
import com.weiy.account.navigation.toAppRoute
import com.weiy.account.navigation.toStartDestinationOrNull
import com.weiy.account.ui.screens.CategoryManageScreen
import com.weiy.account.ui.screens.HomeScreen
import com.weiy.account.ui.screens.OnboardingScreen
import com.weiy.account.ui.screens.RecurringAccountingCreateScreen
import com.weiy.account.ui.screens.RecurringAccountingListScreen
import com.weiy.account.ui.screens.SearchScreen
import com.weiy.account.ui.screens.SettingsScreen
import com.weiy.account.ui.screens.StatsScreen
import com.weiy.account.ui.screens.TransactionEditScreen
import com.weiy.account.ui.screens.TransactionListScreen
import com.weiy.account.viewmodel.CategoryManageViewModel
import com.weiy.account.viewmodel.HomeViewModel
import com.weiy.account.viewmodel.RecurringAccountingCreateViewModel
import com.weiy.account.viewmodel.RecurringAccountingListViewModel
import com.weiy.account.viewmodel.SearchViewModel
import com.weiy.account.viewmodel.SettingsViewModel
import com.weiy.account.viewmodel.StatsViewModel
import com.weiy.account.viewmodel.TransactionEditViewModel
import com.weiy.account.viewmodel.TransactionListViewModel

private data class BottomItem(
    val route: AppRoute,
    val label: String,
    val icon: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMain(
    appContainer: AppContainer,
    settingsViewModel: SettingsViewModel,
    openTransactionEditRequestKey: Int = 0
) {
    val settings by settingsViewModel.uiState.collectAsState()
    val startRoute = remember {
        if (settings.onboardingShown) {
            settings.defaultStartDestination.toAppRoute()
        } else {
            AppRoute.Onboarding
        }
    }
    val backStack = rememberNavBackStack(startRoute)

    val currentRoute = (backStack.lastOrNull() as? AppRoute) ?: startRoute
    val canGoBack = backStack.size > 1
    val showTopBar = currentRoute != AppRoute.Onboarding &&
        currentRoute != AppRoute.CategoryManage &&
        currentRoute != AppRoute.Search &&
        currentRoute !is AppRoute.TransactionEdit
    val showBottomBar = currentRoute.toStartDestinationOrNull() != null
    val isHomeRoute = currentRoute == AppRoute.Home
    var homeFabScrollUpEnabled by remember { mutableStateOf(false) }
    var homeScrollToTopSignal by remember { mutableIntStateOf(0) }
    var handledOpenTransactionEditRequestKey by remember { mutableIntStateOf(0) }
    var fabHiddenByLongPress by rememberSaveable { mutableStateOf(false) }
    var fabLongPressTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(openTransactionEditRequestKey, settings.onboardingShown) {
        if (!settings.onboardingShown) return@LaunchedEffect
        if (openTransactionEditRequestKey == 0) return@LaunchedEffect
        if (openTransactionEditRequestKey == handledOpenTransactionEditRequestKey) return@LaunchedEffect

        handledOpenTransactionEditRequestKey = openTransactionEditRequestKey
        backStack.add(AppRoute.TransactionEdit())
    }

    LaunchedEffect(isHomeRoute) {
        if (isHomeRoute) {
            fabHiddenByLongPress = false
            fabLongPressTriggered = false
        }
    }

    val bottomItems = remember {
        listOf(
            BottomItem(
                route = AppRoute.Home,
                label = "首页",
                icon = { Icon(Icons.Default.Home, contentDescription = "首页") }
            ),
            BottomItem(
                route = AppRoute.TransactionList,
                label = "账单",
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "账单") }
            ),
            BottomItem(
                route = AppRoute.Stats,
                label = "报表",
                icon = { Icon(Icons.Default.Menu, contentDescription = "报表") }
            ),
            BottomItem(
                route = AppRoute.Settings,
                label = "设置",
                icon = { Icon(Icons.Default.Settings, contentDescription = "设置") }
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = routeTitle(currentRoute),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        if (canGoBack) {
                            IconButton(onClick = { backStack.removeLastOrNull() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回"
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentRoute == AppRoute.Home) {
                            IconButton(onClick = { backStack.add(AppRoute.Search) }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                Box {
                    NavigationBar(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        bottomItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        backStack.clear()
                                        backStack.add(item.route)
                                    }
                                },
                                icon = item.icon,
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            val showFab = showBottomBar && (isHomeRoute || !fabHiddenByLongPress)
            if (showFab) {
                val showScrollToTopAction = isHomeRoute && homeFabScrollUpEnabled
                FloatingActionButton(
                    modifier = if (isHomeRoute) {
                        Modifier
                    } else {
                        Modifier.pointerInput(isHomeRoute) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val longPress = awaitLongPressOrCancellation(down.id)
                                if (longPress != null) {
                                    fabHiddenByLongPress = true
                                    fabLongPressTriggered = true
                                    waitForUpOrCancellation()
                                }
                            }
                        }
                    },
                    onClick = {
                        if (fabLongPressTriggered) {
                            fabLongPressTriggered = false
                            return@FloatingActionButton
                        }
                        if (showScrollToTopAction) {
                            homeScrollToTopSignal += 1
                        } else {
                            backStack.add(AppRoute.TransactionEdit())
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (showScrollToTopAction) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.Add
                        },
                        contentDescription = null
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            backStack = backStack,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None
                )
            },
            popTransitionSpec = {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None
                )
            },
            predictivePopTransitionSpec = { _ ->
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None
                )
            },
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider<NavKey> {
                entry<AppRoute.Onboarding> {
                    OnboardingScreen(
                        onFinish = {
                            settingsViewModel.setOnboardingShown(true)
                            val destination = appContainer.settingsRepository.currentSettings
                                .defaultStartDestination
                                .toAppRoute()
                            backStack.clear()
                            backStack.add(destination)
                        }
                    )
                }

                entry<AppRoute.Home> {
                    val vm: HomeViewModel = viewModel(
                        factory = HomeViewModel.factory(appContainer.transactionRepository)
                    )
                    HomeScreen(
                        viewModel = vm,
                        onQuickAdd = { backStack.add(AppRoute.TransactionEdit()) },
                        onOpenTransaction = { id -> backStack.add(AppRoute.TransactionEdit(id)) },
                        onFirstItemCompletelyInvisibleChanged = { invisible ->
                            homeFabScrollUpEnabled = invisible
                        },
                        scrollToTopSignal = homeScrollToTopSignal
                    )
                }

                entry<AppRoute.TransactionList> {
                    val vm: TransactionListViewModel = viewModel(
                        factory = TransactionListViewModel.factory(
                            transactionRepository = appContainer.transactionRepository,
                            settingsRepository = appContainer.settingsRepository
                        )
                    )
                    TransactionListScreen(
                        viewModel = vm,
                        onAddTransaction = { backStack.add(AppRoute.TransactionEdit()) },
                        onOpenTransaction = { id -> backStack.add(AppRoute.TransactionEdit(id)) }
                    )
                }

                entry<AppRoute.Stats> {
                    val vm: StatsViewModel = viewModel(
                        factory = StatsViewModel.factory(appContainer.transactionRepository)
                    )
                    StatsScreen(viewModel = vm)
                }

                entry<AppRoute.Settings> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onOpenCategoryManage = { backStack.add(AppRoute.CategoryManage) },
                        onOpenRecurringAccounting = { backStack.add(AppRoute.RecurringAccountingList) }
                    )
                }

                entry<AppRoute.Search> {
                    val vm: SearchViewModel = viewModel(
                        factory = SearchViewModel.factory(
                            transactionRepository = appContainer.transactionRepository,
                            searchHistoryRepository = appContainer.searchHistoryRepository
                        )
                    )
                    SearchScreen(
                        viewModel = vm,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenTransaction = { id -> backStack.add(AppRoute.TransactionEdit(id)) }
                    )
                }

                entry<AppRoute.RecurringAccountingList> {
                    val vm: RecurringAccountingListViewModel = viewModel(
                        factory = RecurringAccountingListViewModel.factory(
                            repository = appContainer.recurringAccountingRepository
                        )
                    )
                    RecurringAccountingListScreen(
                        viewModel = vm,
                        onAdd = { backStack.add(AppRoute.RecurringAccountingCreate) }
                    )
                }

                entry<AppRoute.RecurringAccountingCreate> {
                    val vm: RecurringAccountingCreateViewModel = viewModel(
                        factory = RecurringAccountingCreateViewModel.factory(
                            recurringRepository = appContainer.recurringAccountingRepository,
                            categoryRepository = appContainer.categoryRepository
                        )
                    )
                    RecurringAccountingCreateScreen(
                        viewModel = vm,
                        onFinished = { backStack.removeLastOrNull() }
                    )
                }

                entry<AppRoute.CategoryManage> {
                    val vm: CategoryManageViewModel = viewModel(
                        factory = CategoryManageViewModel.factory(appContainer.categoryRepository)
                    )
                    CategoryManageScreen(
                        viewModel = vm,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<AppRoute.TransactionEdit> { route ->
                    val vm: TransactionEditViewModel = viewModel(
                        key = "transaction_edit_${route.transactionId}",
                        factory = TransactionEditViewModel.factory(
                            transactionId = route.transactionId,
                            transactionRepository = appContainer.transactionRepository,
                            categoryRepository = appContainer.categoryRepository,
                            settingsRepository = appContainer.settingsRepository
                        )
                    )
                    TransactionEditScreen(
                        viewModel = vm,
                        onFinished = { backStack.removeLastOrNull() },
                        onOpenCategoryManage = { backStack.add(AppRoute.CategoryManage) }
                    )
                }
            }
        )
    }
}

private fun routeTitle(route: AppRoute): String {
    return when (route) {
        AppRoute.Onboarding -> "Onboarding"
        AppRoute.Home -> "唯忆记账"
        AppRoute.TransactionList -> "账单"
        AppRoute.Stats -> "报表"
        AppRoute.Settings -> "设置"
        AppRoute.Search -> "搜索"
        AppRoute.CategoryManage -> "分类管理"
        AppRoute.RecurringAccountingList -> "定时记账"
        AppRoute.RecurringAccountingCreate -> "添加定时记账"
        is AppRoute.TransactionEdit -> if (route.transactionId > 0L) "编辑明细" else "新增明细"
    }
}

