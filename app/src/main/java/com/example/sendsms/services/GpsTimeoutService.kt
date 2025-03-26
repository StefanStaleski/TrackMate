package com.example.sendsms.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sendsms.MainActivity
import com.example.sendsms.R
import com.example.sendsms.utils.SMSScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GpsTimeoutService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var timeoutJob: Job? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationHelper: NotificationHelper
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        private const val TAG = "GpsTimeoutService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "gps_timeout_channel"
        private const val TIMEOUT_DURATION_MS = 90 * 1000L // 90 seconds
        private const val MAX_RETRIES = 2 // Allow 3 attempts total (initial + 2 retries)
        
        fun startService(context: Context, phoneNumber: String) {
            val intent = Intent(context, GpsTimeoutService::class.java).apply {
                putExtra("phoneNumber", phoneNumber)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            Log.d(TAG, "Starting GpsTimeoutService for number: $phoneNumber")
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, GpsTimeoutService::class.java)
            context.stopService(intent)
            Log.d(TAG, "Stopping GpsTimeoutService")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        notificationHelper = NotificationHelper(this)
        createNotificationChannel()
        
        // Create a partial wake lock - this doesn't require special permissions
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GpsTimeoutService::WakeLock"
        )
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("phoneNumber") ?: ""
        
        if (phoneNumber.isBlank()) {
            Log.e(TAG, "No phone number provided, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Start as a foreground service with a silent notification
        startForeground(NOTIFICATION_ID, createSilentNotification())
        
        // Acquire wake lock to help keep service running
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max
        
        // Reset retry count
        sharedPreferences.edit()
            .putInt("gpsPollingRetryCount", 0)
            .putLong("lastGpsPollingAttempt", System.currentTimeMillis())
            .putBoolean("gpsPollingInProgress", true)
            .putString("lastGpsResponseType", "")
            .apply()
        
        // Send initial SMS
        SMSScheduler.scheduleSMS(this, phoneNumber, "777", 0)
        Log.d(TAG, "Sent initial GPS polling SMS to $phoneNumber")
        
        // Start timeout monitoring
        startTimeoutMonitoring(phoneNumber)
        
        return START_STICKY
    }
    
    private fun startTimeoutMonitoring(phoneNumber: String) {
        // Cancel any existing job
        timeoutJob?.cancel()
        
        timeoutJob = serviceScope.launch {
            var retryCount = 0
            
            while (isActive && retryCount <= MAX_RETRIES) {
                // Wait for timeout duration
                delay(TIMEOUT_DURATION_MS)
                
                // Check if we've received a response
                val lastResponseType = sharedPreferences.getString("lastGpsResponseType", "")
                val lastResponseTime = sharedPreferences.getLong("lastGpsResponseTime", 0)
                val lastAttemptTime = sharedPreferences.getLong("lastGpsPollingAttempt", 0)
                
                // If we received a valid response after our last attempt, stop monitoring
                if (lastResponseType == "valid" && lastResponseTime > lastAttemptTime) {
                    Log.d(TAG, "Received valid response, stopping timeout monitoring")
                    stopSelf()
                    break
                }
                
                // If we've reached max retries, send notification and stop
                if (retryCount >= MAX_RETRIES) {
                    Log.d(TAG, "Max retries reached ($retryCount), sending notification")
                    
                    // Reset polling state
                    sharedPreferences.edit()
                        .putBoolean("gpsPollingInProgress", false)
                        .putInt("gpsPollingRetryCount", 0)
                        .apply()
                    
                    // Send notification
                    notificationHelper.sendNotification(
                        "GPS Locator Error",
                        "GPS Locator is not responding after multiple attempts. Please check the device.",
                        NotificationHelper.CHANNEL_GPS_ERROR,
                        "no_response"
                    )
                    
                    // Stop service after a short delay
                    delay(3000)
                    stopSelf()
                    break
                }
                
                // Increment retry count
                retryCount++
                
                // Update retry count in SharedPreferences
                sharedPreferences.edit()
                    .putInt("gpsPollingRetryCount", retryCount)
                    .putLong("lastGpsPollingAttempt", System.currentTimeMillis())
                    .apply()
                
                // Send retry SMS
                Log.d(TAG, "Retrying GPS polling (attempt ${retryCount + 1}/${MAX_RETRIES + 1})")
                SMSScheduler.scheduleSMS(this@GpsTimeoutService, phoneNumber, "777", 0)
            }
        }
    }
    
    private fun createSilentNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Service")
            .setContentText("Running in background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN) // Minimum priority
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Hide from lock screen
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Timeout Service",
                NotificationManager.IMPORTANCE_MIN // Minimum importance
            ).apply {
                description = "Used to monitor GPS polling timeouts"
                setShowBadge(false) // Don't show badge
                enableLights(false) // No lights
                enableVibration(false) // No vibration
                lockscreenVisibility = Notification.VISIBILITY_SECRET // Hide from lock screen
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        timeoutJob?.cancel()
        
        // Release wake lock if it's held
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
} 