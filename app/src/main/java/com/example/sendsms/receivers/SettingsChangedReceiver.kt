package com.example.sendsms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.sendsms.services.PeriodicSmsWorker

class SettingsChangedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SettingsChangedReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Settings changed, rescheduling periodic SMS worker")
        PeriodicSmsWorker.schedulePeriodicSms(context)
    }
} 