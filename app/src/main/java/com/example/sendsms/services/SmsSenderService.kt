package com.example.sendsms.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log

class SmsSenderService : Service() {
    companion object {
        private const val TAG = "SmsSenderService"
        private const val ACTION_SEND_SMS = "com.example.sendsms.ACTION_SEND_SMS"
        private const val EXTRA_PHONE_NUMBER = "phone_number"
        private const val EXTRA_MESSAGE = "message"
        
        fun sendSms(context: Context, phoneNumber: String, message: String) {
            val intent = Intent(context, SmsSenderService::class.java).apply {
                action = ACTION_SEND_SMS
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_MESSAGE, message)
            }
            context.startService(intent)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SEND_SMS) {
            val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
            val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
            
            if (phoneNumber.isNotBlank() && message.isNotBlank()) {
                sendSms(phoneNumber, message)
            }
        }
        
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
