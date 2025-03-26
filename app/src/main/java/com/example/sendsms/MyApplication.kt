package com.example.sendsms

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sendsms.database.AppDatabase
import com.example.sendsms.services.BackgroundService
import com.example.sendsms.services.PeriodicSmsWorker
import java.util.concurrent.TimeUnit

class MyApplication : Application(), Configuration.Provider {
    // Use lazy initialization to ensure the database is only created once
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        
        // Start the background service when the app starts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, BackgroundService::class.java))
        } else {
            startService(Intent(this, BackgroundService::class.java))
        }
        
        // Schedule periodic work with high priority
        schedulePeriodicWork()
    }
    
    private fun schedulePeriodicWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val periodicWorkRequest = PeriodicWorkRequestBuilder<PeriodicSmsWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "periodic_sms_work",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}
