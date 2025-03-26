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
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sendsms.MainActivity
import com.example.sendsms.R

class SmsSenderService : Service() {
    companion object {
        private const val TAG = "SmsSenderService"
        private const val ACTION_SEND_SMS = "com.example.sendsms.ACTION_SEND_SMS"
        private const val EXTRA_PHONE_NUMBER = "phone_number"
        private const val EXTRA_MESSAGE = "message"
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "sms_sender_channel"
        
        fun sendSms(context: Context, phoneNumber: String, message: String) {
            val intent = Intent(context, SmsSenderService::class.java).apply {
                action = ACTION_SEND_SMS
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_MESSAGE, message)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Sender Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used when sending SMS messages"
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
            .setContentText("Sending SMS...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .build()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        if (intent?.action == ACTION_SEND_SMS) {
            val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
            val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
            
            if (phoneNumber.isNotBlank() && message.isNotBlank()) {
                sendSms(phoneNumber, message)
            }
        }
        
        stopForeground(true)
        stopSelf(startId)
        return START_NOT_STICKY
    }
    
    private fun sendSms(phoneNumber: String, message: String) {
        try {
            Log.d(TAG, "Sending SMS to $phoneNumber: $message")
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "SMS sent successfully")
            
            // Update last attempt time in SharedPreferences
            val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putLong("lastGpsPollingAttempt", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}")
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
