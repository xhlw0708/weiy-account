package com.weiy.account.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.weiy.account.MainActivity
import com.weiy.account.R

class AccountingReminderNotifier(context: Context) {

    private val appContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(appContext)

    fun showReminderNotification() {
        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(
                AccountingReminderScheduler.LOG_TAG,
                "showReminderNotification(): POST_NOTIFICATIONS not granted"
            )
            return
        }

        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_panel)
            .setContentTitle("记账提醒")
            .setContentText("记账时间到了，赶快记一笔吧！")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(createContentIntent())
            .build()
        Log.d(
            AccountingReminderScheduler.LOG_TAG,
            "showReminderNotification(): notify id=$NOTIFICATION_ID channel=$CHANNEL_ID"
        )
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = appContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "记账提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "每天定时提醒用户记账"
        }
        Log.d(
            AccountingReminderScheduler.LOG_TAG,
            "ensureNotificationChannel(): createOrUpdate channel=$CHANNEL_ID"
        )
        manager.createNotificationChannel(channel)
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TRANSACTION_EDIT, true)
        }
        return PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_OPEN_TRANSACTION_EDIT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "accounting_reminder"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_OPEN_TRANSACTION_EDIT = 2002
    }
}
