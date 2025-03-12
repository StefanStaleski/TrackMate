package com.example.sendsms.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.sendsms.services.NotificationHelper
import com.example.sendsms.utils.SMSScheduler
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
        private const val TIMEOUT_DURATION_MS = 30 * 1000L // 30 seconds
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
                        handler.postDelayed(timeoutRunnable, TIMEOUT_DURATION_MS)
                    } else {
                        Log.e(TAG, "Cannot retry: GPS locator number is blank")
                        isPolling.set(false)
                    }
                }
            }
        }
    }
    
    /**
     * Start GPS polling with automatic retries.
     * @param phoneNumber The phone number to send the GPS polling SMS to
     */
    fun startPolling(phoneNumber: String) {
        if (isPolling.getAndSet(true)) {
            // Already polling, cancel the previous one first
            cancelPolling()
            isPolling.set(true)
        }
        
        retryCount = 0
        
        // Update SharedPreferences
        sharedPreferences.edit()
            .putInt("gpsPollingRetryCount", 0)
            .putLong("lastGpsPollingAttempt", System.currentTimeMillis())
            .putBoolean("gpsPollingInProgress", true)
            .apply()
        
        // Send the initial SMS
        SMSScheduler.scheduleSMS(context, phoneNumber, "777", 0)
        
        // Schedule the timeout check
        handler.postDelayed(timeoutRunnable, TIMEOUT_DURATION_MS)
    }
    
    /**
     * Cancel any ongoing GPS polling.
     */
    fun cancelPolling() {
        if (isPolling.getAndSet(false)) {
            Log.d(TAG, "Cancelling GPS polling")
            handler.removeCallbacks(timeoutRunnable)
            
            // Reset polling state in SharedPreferences
            sharedPreferences.edit()
                .putBoolean("gpsPollingInProgress", false)
                .apply()
        }
    }
    
    /**
     * Handle a response from the GPS locator.
     * @param isValid Whether the response contains valid GPS data
     */
    fun handleResponse(isValid: Boolean) {
        if (isPolling.get()) {
            handler.removeCallbacks(timeoutRunnable)
            
            if (!isValid) {
                // Invalid response, mark for retry
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
                    
                    Log.d(TAG, "Retrying GPS polling due to invalid data (attempt ${retryCount + 1}/${MAX_RETRIES + 1})")
                    
                    // Send the SMS again
                    val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""
                    if (gpsLocatorNumber.isNotBlank()) {
                        SMSScheduler.scheduleSMS(context, gpsLocatorNumber, "777", 0)
                        
                        // Schedule the next timeout check
                        handler.postDelayed(timeoutRunnable, TIMEOUT_DURATION_MS)
                    } else {
                        Log.e(TAG, "Cannot retry: GPS locator number is blank")
                        isPolling.set(false)
                    }
                }
            } else {
                // Valid response, reset polling state
                Log.d(TAG, "Received valid GPS response, polling complete")
                isPolling.set(false)
                retryCount = 0
                
                sharedPreferences.edit()
                    .putBoolean("gpsPollingInProgress", false)
                    .putInt("gpsPollingRetryCount", 0)
                    .putString("lastGpsResponseType", "valid")
                    .apply()
            }
        }
    }
} 