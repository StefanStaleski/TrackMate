package com.example.sendsms.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import com.example.sendsms.services.SmsSenderService

object SMSScheduler {
    fun scheduleSMS(context: Context, phoneNumber: String, message: String, delayMillis: Long) {
        Log.d("SMSScheduler", "Scheduling SMS to $phoneNumber with message '$message' and delay $delayMillis ms")
        
        // Check if this is a GPS polling message (777)
        if (message == "777") {
            // Set up polling state for manual sends
            val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putBoolean("manualGpsPollingInProgress", true)
                .putLong("lastGpsPollingAttempt", System.currentTimeMillis() + delayMillis)
                .apply()
            
            Log.d("SMSScheduler", "Setting up manual GPS polling state")
        }
        
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            SmsSenderService.sendSms(context, phoneNumber, message)
        }, delayMillis)
    }
} 