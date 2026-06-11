package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "new_emails"
    private const val CHANNEL_NAME = "New Emails"
    private const val CHANNEL_DESC = "Notifications for new incoming email receipts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showEmailNotification(context: Context, emailId: String, senderName: String, subject: String, bodySnippet: String) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("New Mail from $senderName")
            .setContentText(subject)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$subject\n$bodySnippet"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // Use stable hashcode of emailId as notification ID so we can cancel it when read!
            notificationManager.notify(emailId.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Under API 33+ if POST_NOTIFICATIONS runtime permission is missing
            e.printStackTrace()
        }
    }

    fun cancelNotification(context: Context, emailId: String) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(emailId.hashCode())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
