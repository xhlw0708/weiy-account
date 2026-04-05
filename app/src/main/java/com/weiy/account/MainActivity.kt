package com.weiy.account

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weiy.account.data.AppContainer
import com.weiy.account.ui.AppMain
import com.weiy.account.ui.theme.WeiyAccountTheme
import com.weiy.account.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContainer = remember { AppContainer(applicationContext) }
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(appContainer.settingsRepository)
            )
            val settings by settingsViewModel.uiState.collectAsState()

            WeiyAccountTheme(
                darkTheme = settings.darkModeEnabled,
                dynamicColor = true
            ) {
                AppMain(
                    appContainer = appContainer,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
