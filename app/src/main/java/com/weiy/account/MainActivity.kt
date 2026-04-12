package com.weiy.account

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weiy.account.data.AppContainer
import com.weiy.account.ui.AppMain
import com.weiy.account.ui.theme.WeiyAccountTheme
import com.weiy.account.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var openTransactionEditRequestKey by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (consumeOpenTransactionEditExtra(intent)) {
            openTransactionEditRequestKey = 1
        }
        enableEdgeToEdge()
        setContent {
            var appContainer by remember { mutableStateOf<AppContainer?>(null) }
            var startupError by remember { mutableStateOf<Throwable?>(null) }

            LaunchedEffect(Unit) {
                if (appContainer != null || startupError != null) return@LaunchedEffect
                runCatching {
                    withContext(Dispatchers.IO) {
                        AppContainer(applicationContext)
                    }
                }.onSuccess {
                    appContainer = it
                }.onFailure { error ->
                    startupError = error
                    Log.e("MainActivity", "AppContainer init failed", error)
                }
            }

            val container = appContainer
            if (container == null) {
                WeiyAccountTheme(
                    darkTheme = false,
                    dynamicColor = true
                ) {
                    StartupFallbackScreen(startupError = startupError)
                }
            } else {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(
                        settingsRepository = container.settingsRepository,
                        dataTransferRepository = container.dataTransferRepository
                    )
                )
                val settings by settingsViewModel.uiState.collectAsState()

                WeiyAccountTheme(
                    darkTheme = settings.darkModeEnabled,
                    dynamicColor = true
                ) {
                    AppMain(
                        appContainer = container,
                        settingsViewModel = settingsViewModel,
                        openTransactionEditRequestKey = openTransactionEditRequestKey
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (consumeOpenTransactionEditExtra(intent)) {
            openTransactionEditRequestKey += 1
        }
    }

    private fun consumeOpenTransactionEditExtra(intent: Intent?): Boolean {
        val shouldOpen = intent?.getBooleanExtra(EXTRA_OPEN_TRANSACTION_EDIT, false) == true
        if (shouldOpen) {
            intent?.removeExtra(EXTRA_OPEN_TRANSACTION_EDIT)
        }
        return shouldOpen
    }

    companion object {
        const val EXTRA_OPEN_TRANSACTION_EDIT = "extra_open_transaction_edit"
    }
}

@androidx.compose.runtime.Composable
private fun StartupFallbackScreen(startupError: Throwable?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (startupError == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "Startup failed. Please restart the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
