package com.weiy.account.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.weiy.account.preferences.SettingsPreferencesDataSource

class ReminderRestoreReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in RESTORE_ACTIONS) return

        Log.d(
            AccountingReminderScheduler.LOG_TAG,
            "ReminderRestoreReceiver.onReceive() action=$action"
        )
        val settings = SettingsPreferencesDataSource(context).readSettings()
        val scheduler = AccountingReminderScheduler(context)
        if (settings.reminderEnabled) {
            Log.d(
                AccountingReminderScheduler.LOG_TAG,
                "ReminderRestoreReceiver.onReceive(): restore enabled reminder"
            )
            scheduler.scheduleNextReminder(settings)
        } else {
            Log.d(
                AccountingReminderScheduler.LOG_TAG,
                "ReminderRestoreReceiver.onReceive(): reminder disabled, cancel"
            )
            scheduler.cancelReminder()
        }
    }

    companion object {
        private val RESTORE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }
}
