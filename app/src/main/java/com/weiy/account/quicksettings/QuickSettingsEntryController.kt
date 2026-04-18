package com.weiy.account.quicksettings

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.content.ContextCompat
import com.weiy.account.R

object QuickSettingsEntryController {

    private const val LOG_TAG = "QuickSettingsEntry"

    fun syncComponentEnabledState(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager
        val componentName = tileComponentName(appContext)
        val desiredState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        val currentState = packageManager.getComponentEnabledSetting(componentName)
        if (currentState != desiredState) {
            val flags = PackageManager.DONT_KILL_APP or if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                PackageManager.SYNCHRONOUS
            } else {
                0
            }
            packageManager.setComponentEnabledSetting(
                componentName,
                desiredState,
                flags
            )
        }
        if (enabled) {
            runCatching {
                TileService.requestListeningState(appContext, componentName)
            }.onFailure { error ->
                Log.w(LOG_TAG, "requestListeningState failed", error)
            }
        }
    }

    fun requestAddTile(context: Context) {
        val statusBarManager = context.getSystemService(StatusBarManager::class.java) ?: return
        val appContext = context.applicationContext
        val componentName = tileComponentName(appContext)
        syncComponentEnabledState(context = appContext, enabled = true)
        runCatching {
            statusBarManager.requestAddTileService(
                componentName,
                context.getString(R.string.quick_settings_tile_label),
                Icon.createWithResource(appContext, R.drawable.ic_notification_panel),
                ContextCompat.getMainExecutor(context)
            ) { result ->
                Log.d(LOG_TAG, "requestAddTile result=$result")
            }
        }.onFailure { error ->
            Log.w(LOG_TAG, "requestAddTile failed", error)
        }
    }

    private fun tileComponentName(context: Context): ComponentName {
        return ComponentName(context, QuickAddTileService::class.java)
    }
}
