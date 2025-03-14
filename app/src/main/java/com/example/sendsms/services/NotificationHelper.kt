package com.example.sendsms.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sendsms.MainActivity
import com.example.sendsms.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_DEFAULT = "default_channel"
        const val CHANNEL_BATTERY = "battery_channel"
        const val CHANNEL_GPS_ERROR = "gps_error_channel"
        const val CHANNEL_LOCATION = "location_channel"
        
        private const val NOTIFICATION_ID_DEFAULT = 1
        private const val NOTIFICATION_ID_BATTERY = 2
        private const val NOTIFICATION_ID_GPS_ERROR = 3
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_DEFAULT,
                    "Default Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General notifications"
                    enableLights(true)
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_BATTERY,
                    "Battery Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications about GPS Locator battery level"
                    enableLights(true)
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_GPS_ERROR,
                    "GPS Error Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications about GPS Locator errors or timeouts"
                    enableLights(true)
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_LOCATION,
                    "Location Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for GPS locator location alerts"
                }
            )
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
                Log.d("NotificationHelper", "Created notification channel: ${channel.id}")
            }
        }
    }

    fun sendNotification(title: String, message: String, channelId: String = CHANNEL_LOCATION) {
        Log.d("NotificationHelper", "Attempting to send notification: $title - $message on channel $channelId")
        
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("NotificationHelper", "POST_NOTIFICATIONS permission not granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            // Choose notification ID based on channel
            val notificationId = when (channelId) {
                CHANNEL_BATTERY -> NOTIFICATION_ID_BATTERY
                CHANNEL_GPS_ERROR -> NOTIFICATION_ID_GPS_ERROR
                CHANNEL_LOCATION -> 3
                else -> NOTIFICATION_ID_DEFAULT
            }

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
                Log.d("NotificationHelper", "Notification sent successfully with ID: $notificationId")
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error sending notification", e)
        }
    }
}
