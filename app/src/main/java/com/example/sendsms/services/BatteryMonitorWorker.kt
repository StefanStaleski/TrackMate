package com.example.sendsms.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BatteryMonitorWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val sharedPreferences: SharedPreferences = 
        appContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    
    private val notificationHelper = NotificationHelper(appContext)
    
    companion object {
        private const val TAG = "BatteryMonitorWorker"
        private const val BATTERY_THRESHOLD = 20
        private const val BATTERY_NOTIFICATION_KEY = "last_battery_notification_time"
        private const val NOTIFICATION_COOLDOWN = 6 * 60 * 60 * 1000 // 6 hours in milliseconds
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val batteryPercentage = sharedPreferences.getInt("batteryPercentage", -1)
            val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
            
            Log.d(TAG, "Checking GPS locator battery level: $batteryPercentage%")
            
            if (!isLoggedIn || batteryPercentage == -1) {
                Log.d(TAG, "User not logged in or no GPS locator battery data available")
                return@withContext Result.success()
            }
            
            // Check if GPS locator battery is low and we haven't sent a notification recently
            if (batteryPercentage <= BATTERY_THRESHOLD) {
                val lastNotificationTime = sharedPreferences.getLong(BATTERY_NOTIFICATION_KEY, 0)
                val currentTime = System.currentTimeMillis()
                
                // Only send notification if we haven't sent one in the last 6 hours
                if (currentTime - lastNotificationTime > NOTIFICATION_COOLDOWN) {
                    Log.d(TAG, "GPS locator battery level is low ($batteryPercentage%). Sending notification.")
                    
                    notificationHelper.sendNotification(
                        "GPS Locator Battery Low",
                        "Your GPS locator's battery level is at $batteryPercentage%. Please charge your GPS locator device soon.",
                        NotificationHelper.CHANNEL_BATTERY
                    )
                    
                    // Update last notification time
                    sharedPreferences.edit()
                        .putLong(BATTERY_NOTIFICATION_KEY, currentTime)
                        .apply()
                } else {
                    Log.d(TAG, "GPS locator battery is low but notification was sent recently. Skipping.")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error monitoring GPS locator battery", e)
            Result.failure()
        }
    }
} 