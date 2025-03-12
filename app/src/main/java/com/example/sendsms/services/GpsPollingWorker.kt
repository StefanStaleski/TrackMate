package com.example.sendsms.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sendsms.utils.GpsPollingManager
import kotlinx.coroutines.delay

class GpsPollingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val sharedPreferences: SharedPreferences = 
        appContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        
        if (!isLoggedIn || gpsLocatorNumber.isBlank()) {
            Log.d("GpsPollingWorker", "User not logged in or no GPS locator number set")
            return Result.success()
        }

        Log.d("GpsPollingWorker", "Starting GPS polling to number: $gpsLocatorNumber")
        
        // Use the GpsPollingManager to handle the polling with automatic retries
        val gpsPollingManager = GpsPollingManager.getInstance(applicationContext)
        gpsPollingManager.startPolling(gpsLocatorNumber)
        
        return Result.success()
    }
} 