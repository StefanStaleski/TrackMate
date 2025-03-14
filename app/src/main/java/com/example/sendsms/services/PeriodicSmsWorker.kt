package com.example.sendsms.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.*
import com.example.sendsms.utils.SMSScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PeriodicSmsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val sharedPreferences: SharedPreferences = 
        appContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "PeriodicSmsWorker"
        
        // Function to schedule the worker with the correct frequency
        fun schedulePeriodicSms(context: Context) {
            val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            val isEnabled = sharedPreferences.getBoolean("periodicSmsEnabled", false)
            
            if (!isEnabled) {
                // Cancel any existing work
                WorkManager.getInstance(context).cancelUniqueWork("periodic_sms")
                Log.d(TAG, "Periodic SMS disabled, cancelling worker")
                return
            }
            
            // Get the frequency in minutes (default to 60 minutes)
            val frequencyMinutes = sharedPreferences.getInt("smsFrequencyMinutes", 60)
            
            Log.d(TAG, "Scheduling periodic SMS worker with frequency: $frequencyMinutes minutes")
            
            val periodicWorkRequest = PeriodicWorkRequestBuilder<PeriodicSmsWorker>(
                frequencyMinutes.toLong(), TimeUnit.MINUTES,
                (frequencyMinutes / 10).toLong(), TimeUnit.MINUTES // Flex period of 10% of the interval
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "periodic_sms",
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest
            )
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""
            val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
            val isPeriodicSmsEnabled = sharedPreferences.getBoolean("periodicSmsEnabled", false)
            val periodicSmsMessage = sharedPreferences.getString("periodicSmsMessage", "777") ?: "777"
            
            if (!isLoggedIn || gpsLocatorNumber.isBlank() || !isPeriodicSmsEnabled) {
                Log.d(TAG, "Periodic SMS not sent: User not logged in, no GPS locator number set, or feature disabled")
                return@withContext Result.success()
            }

            Log.d(TAG, "Sending periodic SMS to $gpsLocatorNumber with message: $periodicSmsMessage")
            
            // Use the SMSScheduler to send the message
            SMSScheduler.scheduleSMS(applicationContext, gpsLocatorNumber, periodicSmsMessage, 0)
            
            // Update last sent time
            sharedPreferences.edit()
                .putLong("lastPeriodicSmsSent", System.currentTimeMillis())
                .apply()
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending periodic SMS", e)
            Result.failure()
        }
    }
} 