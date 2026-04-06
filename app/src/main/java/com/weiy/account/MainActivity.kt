package com.weiy.account

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.weiy.account.data.AppContainer
import com.weiy.account.ui.AppMain
import com.weiy.account.ui.theme.WeiyAccountTheme
import com.weiy.account.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
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
                    factory = SettingsViewModel.factory(container.settingsRepository)
                )
                val settings by settingsViewModel.uiState.collectAsState()

                WeiyAccountTheme(
                    darkTheme = settings.darkModeEnabled,
                    dynamicColor = true
                ) {
                    AppMain(
                        appContainer = container,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
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

