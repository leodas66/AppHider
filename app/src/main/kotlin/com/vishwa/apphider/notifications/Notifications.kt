package com.vishwa.apphider.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.annotation.SuppressLint
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vishwa.apphider.R

class Notifications(private val context: Context) {

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun show(unhiddenCount: Int) {
        if (!canNotify()) return
        val intent = Intent(context, HideAllReceiver::class.java).setAction(HideAllReceiver.ACTION)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val action = PendingIntent.getBroadcast(context, 0, intent, flags)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_title) + " ($unhiddenCount)")
            .addAction(android.R.drawable.ic_lock_lock, context.getString(R.string.panic_hide), action)
            .setOngoing(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun watcherNotification(): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setContentTitle(context.getString(R.string.watcher_title))
        .setContentText(context.getString(R.string.watcher_body))
        .setOngoing(true)
        .build()

    @SuppressLint("MissingPermission")
    fun showRehideError() {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(context.getString(R.string.rehide_error_title))
            .setContentText(context.getString(R.string.rehide_error_body))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(REHIDE_ERROR_NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    fun showUpdateAvailable(versionName: String) {
        if (!canNotify()) return
        val intent = Intent(context, com.vishwa.apphider.MainActivity::class.java).apply {
            action = com.vishwa.apphider.MainActivity.ACTION_SECRET_UNLOCK
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, UPDATE_REQUEST_CODE, intent, flags)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(context.getString(R.string.update_notification_title))
            .setContentText(context.getString(R.string.update_notification_body, versionName))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
    }

    fun dismiss() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(REHIDE_ERROR_NOTIFICATION_ID)
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val WATCHER_NOTIFICATION_ID = 3
        private const val CHANNEL_ID = "unhidden"
        private const val NOTIFICATION_ID = 1
        private const val REHIDE_ERROR_NOTIFICATION_ID = 4
        private const val UPDATE_NOTIFICATION_ID = 2
        private const val UPDATE_REQUEST_CODE = 2
    }
}
