package com.weiy.account.navigation

import androidx.navigation3.runtime.NavKey
import com.weiy.account.model.StartDestination
import kotlinx.serialization.Serializable

sealed interface AppRoute : NavKey {

    @Serializable
    data object Onboarding : AppRoute

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object TransactionList : AppRoute

    @Serializable
    data object Stats : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data class TransactionEdit(val transactionId: Long = 0L) : AppRoute

    @Serializable
    data object CategoryManage : AppRoute

    @Serializable
    data object RecurringAccountingList : AppRoute

    @Serializable
    data object RecurringAccountingCreate : AppRoute
}

fun StartDestination.toAppRoute(): AppRoute {
    return when (this) {
        StartDestination.HOME -> AppRoute.Home
        StartDestination.LIST -> AppRoute.TransactionList
        StartDestination.STATS -> AppRoute.Stats
        StartDestination.SETTINGS -> AppRoute.Settings
    }
}

fun AppRoute.toStartDestinationOrNull(): StartDestination? {
    return when (this) {
        AppRoute.Home -> StartDestination.HOME
        AppRoute.TransactionList -> StartDestination.LIST
        AppRoute.Stats -> StartDestination.STATS
        AppRoute.Settings -> StartDestination.SETTINGS
        else -> null
    }
}
