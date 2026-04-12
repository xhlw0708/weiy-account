package com.weiy.account.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.weiy.account.preferences.SettingsPreferencesDataSource

class AccountingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(
            AccountingReminderScheduler.LOG_TAG,
            "AccountingReminderReceiver.onReceive() action=${intent?.action}"
        )
        if (intent?.action != AccountingReminderScheduler.ACTION_ACCOUNTING_REMINDER) {
            Log.d(
                AccountingReminderScheduler.LOG_TAG,
                "AccountingReminderReceiver.onReceive(): ignored unexpected action"
            )
            return
        }

        val notifier = AccountingReminderNotifier(context)
        notifier.showReminderNotification()

        val settings = SettingsPreferencesDataSource(context).readSettings()
        Log.d(
            AccountingReminderScheduler.LOG_TAG,
            "AccountingReminderReceiver.onReceive(): reschedule enabled=${settings.reminderEnabled} time=%02d:%02d"
                .format(settings.reminderHour, settings.reminderMinute)
        )
        AccountingReminderScheduler(context).scheduleNextReminder(settings)
    }
}
