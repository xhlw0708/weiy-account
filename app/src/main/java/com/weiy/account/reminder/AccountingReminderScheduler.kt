package com.weiy.account.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.weiy.account.MainActivity
import com.weiy.account.model.AppSettings
import java.time.ZonedDateTime

class AccountingReminderScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun scheduleNextReminder(settings: AppSettings) {
        if (!settings.reminderEnabled) {
            Log.d(
                LOG_TAG,
                "scheduleNextReminder(settings): disabled, cancel existing reminder"
            )
            cancelReminder()
            return
        }
        Log.d(
            LOG_TAG,
            "scheduleNextReminder(settings): enabled=true time=%02d:%02d"
                .format(settings.reminderHour, settings.reminderMinute)
        )
        scheduleNextReminder(settings.reminderHour, settings.reminderMinute)
    }

    fun scheduleNextReminder(hour: Int, minute: Int) {
        val triggerAtMillis = calculateNextTriggerAtMillis(
            now = ZonedDateTime.now(),
            hour = hour,
            minute = minute
        )
        val pendingIntent = createReminderPendingIntent()
        val schedulingMode = if (canScheduleExactReminders()) "alarmClock" else "allowWhileIdle"
        Log.d(
            LOG_TAG,
            "scheduleNextReminder(hour, minute): time=%02d:%02d triggerAt=%d mode=%s"
                .format(hour, minute, triggerAtMillis, schedulingMode)
        )
        if (canScheduleExactReminders()) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(
                    triggerAtMillis,
                    createOpenAppPendingIntent()
                ),
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder() {
        Log.d(LOG_TAG, "cancelReminder()")
        alarmManager.cancel(createReminderPendingIntent())
    }

    fun canScheduleExactReminders(): Boolean {
        val canSchedule = alarmManager.canScheduleExactAlarms()
        Log.d(LOG_TAG, "canScheduleExactReminders()=$canSchedule")
        return canSchedule
    }

    fun createExactAlarmSettingsIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.fromParts("package", appContext.packageName, null)
        }
    }

    private fun createReminderPendingIntent(): PendingIntent {
        val intent = Intent(appContext, AccountingReminderReceiver::class.java).apply {
            action = ACTION_ACCOUNTING_REMINDER
            setPackage(appContext.packageName)
        }
        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE_ACCOUNTING_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val LOG_TAG = "AccountingReminder"
        private const val REQUEST_CODE_ACCOUNTING_REMINDER = 2001
        private const val REQUEST_CODE_OPEN_APP = 2003
        internal const val ACTION_ACCOUNTING_REMINDER =
            "com.weiy.account.action.ACCOUNTING_REMINDER"

        internal fun calculateNextTriggerAtMillis(
            now: ZonedDateTime,
            hour: Int,
            minute: Int
        ): Long {
            val nextTrigger = now
                .withHour(hour.coerceIn(0, 23))
                .withMinute(minute.coerceIn(0, 59))
                .withSecond(0)
                .withNano(0)
                .let { scheduled ->
                    if (scheduled.isAfter(now)) scheduled else scheduled.plusDays(1)
                }
            return nextTrigger.toInstant().toEpochMilli()
        }
    }
}
