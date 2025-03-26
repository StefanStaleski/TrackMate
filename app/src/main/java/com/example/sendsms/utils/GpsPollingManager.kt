package com.example.sendsms.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sendsms.services.GpsTimeoutWorker
import com.example.sendsms.services.NotificationHelper
import com.example.sendsms.services.GpsTimeoutService
import com.example.sendsms.utils.SMSScheduler
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A manager class that handles GPS polling with automatic retries using a timer-based approach.
 */
class GpsPollingManager private constructor(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    
    private val notificationHelper = NotificationHelper(context)
    private val handler = Handler(Looper.getMainLooper())
    private val isPolling = AtomicBoolean(false)
    private var retryCount = 0
    
    companion object {
        private const val TAG = "GpsPollingManager"
        private const val TIMEOUT_DURATION_MS = 60 * 1000L // 60 seconds (increased from 30)
        private const val MAX_RETRIES = 2 // Allow 3 attempts total (initial + 2 retries)
        private const val GPS_NOTIFICATION_KEY = "last_gps_error_notification_time"
        
        @Volatile
        private var INSTANCE: GpsPollingManager? = null
        
        fun getInstance(context: Context): GpsPollingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GpsPollingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Define the runnable using lateinit to avoid recursive initialization
    private lateinit var timeoutRunnable: Runnable
    
    init {
        // Initialize the runnable in the init block to avoid recursive reference issues
        timeoutRunnable = Runnable {
            if (isPolling.get()) {
                Log.d(TAG, "GPS polling timed out after ${TIMEOUT_DURATION_MS/1000} seconds")
                
                // Mark as no response
                sharedPreferences.edit()
                    .putString("lastGpsResponseType", "none")
                    .apply()
                
                if (retryCount >= MAX_RETRIES) {
                    // Max retries reached, send notification
                    Log.d(TAG, "Max retries reached ($retryCount), sending notification")
                    
                    // Reset polling state
                    isPolling.set(false)
                    retryCount = 0
                    
                    sharedPreferences.edit()
                        .putBoolean("gpsPollingInProgress", false)
                        .putInt("gpsPollingRetryCount", 0)
                        .apply()
                    
                    notificationHelper.sendNotification(
                        "GPS Locator Error",
                        "GPS Locator is not responding after multiple attempts. Please check the device.",
                        NotificationHelper.CHANNEL_GPS_ERROR
                    )
                    
                    // Update last notification time
                    sharedPreferences.edit()
                        .putLong(GPS_NOTIFICATION_KEY, System.currentTimeMillis())
                        .apply()
                } else {
                    // Retry
                    retryCount++
                    
                    // Update retry count in SharedPreferences
                    sharedPreferences.edit()
                        .putInt("gpsPollingRetryCount", retryCount)
                        .putLong("lastGpsPollingAttempt", System.currentTimeMillis())
                        .apply()
                    
                    Log.d(TAG, "Retrying GPS polling (attempt ${retryCount + 1}/${MAX_RETRIES + 1})")
                    
                    // Send the SMS again
                    val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""
                    if (gpsLocatorNumber.isNotBlank()) {
                        SMSScheduler.scheduleSMS(context, gpsLocatorNumber, "777", 0)
                        
                        // Schedule the next timeout check
                        handler.removeCallbacks(timeoutRunnable) // Remove any existing callbacks first
                        handler.postDelayed(timeoutRunnable, TIMEOUT_DURATION_MS)
                        
                        // Also schedule a backup worker as a failsafe
                        GpsTimeoutWorker.scheduleTimeoutCheck(context)
                    } else {
                        Log.e(TAG, "Cannot retry: GPS locator number is blank")
                        isPolling.set(false)
                    }
                }
            }
        }
    }
    
    /**
     * Schedule a backup timeout worker to ensure timeout is detected even if the app is killed
     */
    private fun scheduleTimeoutWorker() {
        val timeoutWorkRequest = OneTimeWorkRequestBuilder<GpsTimeoutWorker>()
            .setInitialDelay(TIMEOUT_DURATION_MS + 5000, TimeUnit.MILLISECONDS) // Add 5 seconds buffer
            .build()
        
        WorkManager.getInstance(context).enqueue(timeoutWorkRequest)
    }
    
    /**
     * Start GPS polling with automatic retries.
     * @param phoneNumber The phone number to send the GPS polling SMS to
     */
    fun startPolling(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            Log.e(TAG, "Cannot start polling: phone number is blank")
            return
        }
        
        if (isPolling.getAndSet(true)) {
            Log.d(TAG, "GPS polling already in progress, cancelling previous and starting new")
            // Cancel any existing polling
            cancelPolling()
        }
        
        Log.d(TAG, "Starting GPS polling to $phoneNumber")
        
        // Reset retry count
        retryCount = 0
        
        // Update polling status in SharedPreferences
        sharedPreferences.edit()
            .putBoolean("gpsPollingInProgress", true)
            .putInt("gpsPollingRetryCount", 0)
            .putLong("lastGpsPollingAttempt", System.currentTimeMillis())
            .apply()
        
        // Start the timeout service which will handle sending the SMS and retries
        GpsTimeoutService.startService(context, phoneNumber)
    }
    
    /**
     * Cancel any ongoing GPS polling.
     */
    fun cancelPolling() {
        if (isPolling.getAndSet(false)) {
            Log.d(TAG, "Cancelling GPS polling")
            
            // Stop the timeout service
            GpsTimeoutService.stopService(context)
            
            // Reset polling state in SharedPreferences
            sharedPreferences.edit()
                .putBoolean("gpsPollingInProgress", false)
                .apply()
        }
    }
    
    /**
     * Handle a response from the GPS locator.
     * @param isValid Whether the response contained valid coordinates
     * @param isSpecificallyInvalid Whether the response contained specifically -1,-1 coordinates
     */
    fun handleResponse(isValid: Boolean, isSpecificallyInvalid: Boolean = false) {
        Log.d(TAG, "Handling GPS response: isValid=$isValid, isSpecificallyInvalid=$isSpecificallyInvalid")
        
        // Cancel the timeout handler
        handler.removeCallbacks(timeoutRunnable)
        
        if (isSpecificallyInvalid) {
            // This is handled separately in SmsReceiver
            Log.d(TAG, "Received specifically invalid GPS response (-1,-1)")
            
            sharedPreferences.edit()
                .putString("lastGpsResponseType", "specifically_invalid")
                .apply()
            
            // We don't reset polling state here because SmsReceiver will handle retries
        } else if (isValid) {
            // Valid response, reset polling state
            Log.d(TAG, "Received valid GPS response, resetting polling state")
            
            isPolling.set(false)
            retryCount = 0
            
            sharedPreferences.edit()
                .putBoolean("gpsPollingInProgress", false)
                .putInt("gpsPollingRetryCount", 0)
                .putString("lastGpsResponseType", "valid")
                .apply()
        } else {
            // Invalid response but not -1,-1, handle as before
            Log.d(TAG, "Received invalid GPS response")
            
            sharedPreferences.edit()
                .putString("lastGpsResponseType", "invalid")
                .apply()
            
            if (retryCount >= MAX_RETRIES) {
                // Max retries reached, send notification
                Log.d(TAG, "Max retries reached with invalid data, sending notification")
                
                // Reset polling state
                isPolling.set(false)
                retryCount = 0
                
                sharedPreferences.edit()
                    .putBoolean("gpsPollingInProgress", false)
                    .putInt("gpsPollingRetryCount", 0)
                    .apply()
                
                notificationHelper.sendNotification(
                    "GPS Locator Error",
                    "GPS Locator is sending invalid data after multiple attempts. Please check the device.",
                    NotificationHelper.CHANNEL_GPS_ERROR,
                    "no_response"  // Add notification type
                )
                
                // Update last notification time
                sharedPreferences.edit()
                    .putLong(GPS_NOTIFICATION_KEY, System.currentTimeMillis())
                    .apply()
            } else {
                // Retry
                retryCount++
                
                // Update retry count in SharedPreferences
                sharedPreferences.edit()
                    .putInt("gpsPollingRetryCount", retryCount)
                    .putLong("lastGpsPollingAttempt", System.currentTimeMillis())
                    .apply()
                
                Log.d(TAG, "Retrying GPS polling due to invalid data (attempt ${retryCount + 1}/${MAX_RETRIES + 1})")
                
                // Send the SMS again
                val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""
                if (gpsLocatorNumber.isNotBlank()) {
                    SMSScheduler.scheduleSMS(context, gpsLocatorNumber, "777", 0)
                    
                    // Schedule the next timeout check
                    handler.postDelayed(timeoutRunnable, TIMEOUT_DURATION_MS)
                    
                    // Also schedule a backup worker as a failsafe
                    scheduleTimeoutWorker()
                } else {
                    Log.e(TAG, "Cannot retry: GPS locator number is blank")
                    isPolling.set(false)
                }
            }
        }
    }
} 