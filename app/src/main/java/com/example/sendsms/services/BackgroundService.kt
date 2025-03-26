package com.example.sendsms.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sendsms.MainActivity
import com.example.sendsms.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BackgroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private val TAG = "BackgroundService"
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "background_service_channel"
        
        fun startService(context: Context) {
            val intent = Intent(context, BackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, BackgroundService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Create a wake lock to keep the CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SendSMS:BackgroundServiceWakeLock"
        )
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start the heartbeat to keep the service alive
        startHeartbeat()
    }
    
    private fun startHeartbeat() {
        serviceScope.launch {
            while (isActive) {
                Log.d(TAG, "Service heartbeat")
                
                // Check if any scheduled SMS needs to be sent
                checkPendingSms()
                
                // Also check for notifications that need to be sent
                checkNotifications()
                
                delay(60000) // Heartbeat every minute
            }
        }
    }
    
    private fun checkPendingSms() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val lastAttemptTime = sharedPreferences.getLong("lastGpsPollingAttempt", 0)
        val currentTime = System.currentTimeMillis()
        val pollingInterval = sharedPreferences.getLong("gpsPollingInterval", 3600000) // Default 1 hour
        
        if (currentTime - lastAttemptTime > pollingInterval) {
            Log.d(TAG, "Time to send a scheduled SMS")
            val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""
            if (gpsLocatorNumber.isNotBlank()) {
                // Send SMS
                SmsSenderService.sendSms(this, gpsLocatorNumber, "777")
            }
        }
    }
    
    private fun checkNotifications() {
        try {
            val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getInt("userId", -1)
            val batteryPercentage = sharedPreferences.getInt("batteryPercentage", -1)
            
            // Check battery level
            if (batteryPercentage <= 20) {
                val lastNotificationTime = sharedPreferences.getLong("last_battery_notification_time", 0)
                val currentTime = System.currentTimeMillis()
                
                // Only send notification if we haven't sent one in the last 6 hours
                if (currentTime - lastNotificationTime > 6 * 60 * 60 * 1000) {
                    val notificationHelper = NotificationHelper(this)
                    notificationHelper.sendNotification(
                        "GPS Locator Battery Low",
                        "Your GPS locator's battery level is at $batteryPercentage%. Please charge your GPS locator device soon.",
                        NotificationHelper.CHANNEL_BATTERY,
                        "battery_low"
                    )
                    
                    // Update last notification time
                    sharedPreferences.edit()
                        .putLong("last_battery_notification_time", currentTime)
                        .apply()
                }
            }
            
            // Trigger the notification worker to check location-based notifications
            val oneTimeRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
            WorkManager.getInstance(this).enqueue(oneTimeRequest)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notifications", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the app running in the background"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TrackMate")
            .setContentText("Running in background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        serviceScope.cancel()
        wakeLock?.release()
        
        // Restart the service if it was killed
        val restartIntent = Intent(applicationContext, BackgroundService::class.java)
        startService(restartIntent)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
} 