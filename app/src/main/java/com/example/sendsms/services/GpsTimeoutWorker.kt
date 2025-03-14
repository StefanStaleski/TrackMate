package com.example.sendsms.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.sendsms.utils.SMSScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class GpsTimeoutWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val sharedPreferences: SharedPreferences = 
        appContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    
    private val notificationHelper = NotificationHelper(appContext)
    
    companion object {
        private const val TAG = "GpsTimeoutWorker"
        private const val TIMEOUT_DURATION_MS = 60 * 1000L // 60 seconds timeout
        private const val MAX_RETRIES = 2 // Allow 3 attempts total (initial + 2 retries)
        private const val GPS_NOTIFICATION_KEY = "last_gps_error_notification_time"
        private const val NOTIFICATION_COOLDOWN = 2 * 60 * 60 * 1000 // 2 hours in milliseconds
        
        // Add this method to schedule the worker from other classes
        fun scheduleTimeoutCheck(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<GpsTimeoutWorker>()
                .setInitialDelay(TIMEOUT_DURATION_MS, TimeUnit.MILLISECONDS)
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Scheduled one-time GpsTimeoutWorker to run after ${TIMEOUT_DURATION_MS/1000} seconds")
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pollingInProgress = sharedPreferences.getBoolean("gpsPollingInProgress", false)
            val manualPollingInProgress = sharedPreferences.getBoolean("manualGpsPollingInProgress", false)
            val lastResponseType = sharedPreferences.getString("lastGpsResponseType", "")
            
            Log.d(TAG, "GpsTimeoutWorker running: pollingInProgress=$pollingInProgress, " +
                    "manualPollingInProgress=$manualPollingInProgress, " +
                    "lastResponseType=$lastResponseType")
            
            // Check if we're in a polling state or if the last response was invalid
            if (!pollingInProgress && !manualPollingInProgress && lastResponseType != "invalid") {
                Log.d(TAG, "No polling in progress, exiting")
                return@withContext Result.success()
            }
            
            val lastAttempt = sharedPreferences.getLong("lastGpsPollingAttempt", 0L)
            val currentTime = System.currentTimeMillis()
            val retryCount = sharedPreferences.getInt("gpsPollingRetryCount", 0)
            val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""
            
            Log.d(TAG, "Checking timeout: lastAttempt=$lastAttempt, currentTime=$currentTime, " +
                    "diff=${(currentTime - lastAttempt) / 1000}s, retryCount=$retryCount")
            
            // If it's been more than the timeout duration since the last attempt and we're still waiting
            if (currentTime - lastAttempt > TIMEOUT_DURATION_MS) {
                Log.d(TAG, "GPS polling timed out after ${TIMEOUT_DURATION_MS/1000} seconds")
                
                // Mark as no response
                sharedPreferences.edit()
                    .putString("lastGpsResponseType", "none")
                    .apply()
                
                // If we've already tried the maximum number of times, send notification
                if (retryCount >= MAX_RETRIES) {
                    Log.d(TAG, "Max retries reached ($retryCount), sending notification")
                    
                    // Reset polling state
                    sharedPreferences.edit()
                        .putBoolean("gpsPollingInProgress", false)
                        .putBoolean("manualGpsPollingInProgress", false)
                        .putInt("gpsPollingRetryCount", 0)
                        .putString("lastGpsResponseType", "")  // Clear response type too
                        .apply()
                    
                    // Send notification about failure
                    notificationHelper.sendNotification(
                        "GPS Locator Error",
                        "GPS Locator is not responding after multiple attempts. Please check the device.",
                        NotificationHelper.CHANNEL_GPS_ERROR
                    )
                    
                    // Update last notification time
                    sharedPreferences.edit()
                        .putLong(GPS_NOTIFICATION_KEY, currentTime)
                        .apply()
                    
                    return@withContext Result.success()
                }
                
                // Increment retry count
                val newRetryCount = retryCount + 1
                sharedPreferences.edit()
                    .putInt("gpsPollingRetryCount", newRetryCount)
                    .putLong("lastGpsPollingAttempt", currentTime)
                    .apply()
                
                Log.d(TAG, "Retrying GPS polling directly (attempt ${newRetryCount}/${MAX_RETRIES + 1})")
                
                // Send the SMS again immediately
                if (gpsLocatorNumber.isNotBlank()) {
                    SMSScheduler.scheduleSMS(applicationContext, gpsLocatorNumber, "777", 0)
                    Log.d(TAG, "Sent retry SMS to $gpsLocatorNumber")
                } else {
                    Log.e(TAG, "Cannot retry: GPS locator number is blank")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking GPS timeout", e)
            Result.failure()
        }
    }
} 