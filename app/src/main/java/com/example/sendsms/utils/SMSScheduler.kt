package com.example.sendsms.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

object SMSScheduler {
    fun scheduleSMS(context: Context, phoneNumber: String, message: String, delaySeconds: Int) {
        Log.d("SMSScheduler", "Scheduling SMS to $phoneNumber with message: $message, delay: $delaySeconds seconds")
        
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                sendSMS(phoneNumber, message)
                Log.d("SMSScheduler", "SMS sent to $phoneNumber")
            } catch (e: Exception) {
                Log.e("SMSScheduler", "Failed to send SMS: ${e.message}")
            }
        }, delaySeconds * 1000L)
    }
} 