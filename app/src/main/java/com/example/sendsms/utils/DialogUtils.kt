package com.example.sendsms.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager

object DialogUtils {
    const val ACTION_SHOW_SMS_DIALOG = "com.example.sendsms.SHOW_SMS_DIALOG"
    const val EXTRA_SMS_MESSAGE = "sms_message"
    const val EXTRA_SMS_SENDER = "sms_sender"

    fun showSmsResponseDialog(context: Context, sender: String, message: String) {
        Log.d("DialogUtils", "Attempting to show dialog for message from $sender")
        
        // Show a Toast as a fallback
        Toast.makeText(
            context,
            "GPS Locator: $message",
            Toast.LENGTH_LONG
        ).show()
        
        // Send broadcast to show dialog
        val intent = Intent(ACTION_SHOW_SMS_DIALOG).apply {
            putExtra(EXTRA_SMS_SENDER, sender)
            putExtra(EXTRA_SMS_MESSAGE, message)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
} 