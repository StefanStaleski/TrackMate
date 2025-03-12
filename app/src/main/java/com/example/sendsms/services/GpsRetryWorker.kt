package com.example.sendsms.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sendsms.utils.SMSScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GpsRetryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val sharedPreferences: SharedPreferences = 
        appContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    
    private val notificationHelper = NotificationHelper(appContext)
    
    companion object {
        private const val TAG = "GpsRetryWorker"
        private const val MAX_RETRIES = 2 // Allow 3 attempts total (initial + 2 retries)
        private const val GPS_NOTIFICATION_KEY = "last_gps_error_notification_time"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""
            val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
            val retryCount = sharedPreferences.getInt("gpsPollingRetryCount", 0)
            val pollingInProgress = sharedPreferences.getBoolean("gpsPollingInProgress", false)
            
            if (!isLoggedIn || gpsLocatorNumber.isBlank() || !pollingInProgress) {
                return@withContext Result.success()
            }
            
            // Check if we've reached the maximum retry count
            if (retryCount >= MAX_RETRIES) {
                // Reset polling state
                sharedPreferences.edit()
                    .putBoolean("gpsPollingInProgress", false)
                    .putInt("gpsPollingRetryCount", 0)
                    .apply()
                
                // Send notification about failure
                val lastResponseType = sharedPreferences.getString("lastGpsResponseType", "none")
                val currentTime = System.currentTimeMillis()
                
                // Update last notification time
                sharedPreferences.edit()
                    .putLong(GPS_NOTIFICATION_KEY, currentTime)
                    .apply()
                
                if (lastResponseType == "invalid") {
                    notificationHelper.sendNotification(
                        "GPS Locator Error",
                        "GPS Locator is sending invalid data after multiple attempts. Please check the device.",
                        NotificationHelper.CHANNEL_GPS_ERROR
                    )
                } else {
                    notificationHelper.sendNotification(
                        "GPS Locator Error",
                        "GPS Locator is not responding after multiple attempts. Please check the device.",
                        NotificationHelper.CHANNEL_GPS_ERROR
                    )
                }
                
                return@withContext Result.success()
            }
            
            // Increment retry count
            sharedPreferences.edit()
                .putInt("gpsPollingRetryCount", retryCount + 1)
                .putLong("lastGpsPollingAttempt", System.currentTimeMillis())
                .apply()
            
            Log.d(TAG, "Retrying GPS polling (attempt ${retryCount + 1}/${MAX_RETRIES + 1})")
            
            // Send the SMS again
            SMSScheduler.scheduleSMS(applicationContext, gpsLocatorNumber, "777", 0)
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in GPS retry", e)
            Result.failure()
        }
    }
} 