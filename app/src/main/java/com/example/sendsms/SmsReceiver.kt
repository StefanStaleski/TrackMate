package com.example.sendsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sendsms.database.AppDatabase
import com.example.sendsms.database.entity.GPSData
import com.example.sendsms.database.repository.GPSDataRepository
import com.example.sendsms.services.BatteryMonitorWorker
import com.example.sendsms.services.GpsRetryWorker
import com.example.sendsms.utils.DialogUtils
import com.example.sendsms.utils.SmsParser
import com.example.sendsms.utils.GpsPollingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (message in messages) {
                val sender = message.originatingAddress ?: "Unknown"
                val messageBody = message.messageBody
                
                Log.d("SmsReceiver", "SMS received from: $sender, message: $messageBody")
                
                // Broadcast the message to the app
                val broadcastIntent = Intent("com.example.sendsms.SMS_RECEIVED")
                broadcastIntent.putExtra("message", messageBody)
                context.sendBroadcast(broadcastIntent)
                
                // Show dialog with the message
                DialogUtils.showSmsResponseDialog(context, sender, messageBody)
                
                // Process the message if it looks like a GPS locator message
                if (messageBody.contains("VBT:") && messageBody.contains("www.google.com/maps")) {
                    processGpsLocatorMessage(context, sender, messageBody)
                }
            }
        }
    }
    
    private fun processGpsLocatorMessage(context: Context, sender: String, message: String) {
        val parsedData = SmsParser.parseGpsLocatorSms(message)
        
        // Get user ID from SharedPreferences
        val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)
        val pollingInProgress = sharedPreferences.getBoolean("gpsPollingInProgress", false)
        
        // Update polling status in SharedPreferences
        if (pollingInProgress) {
            // Notify the polling manager about the response
            val gpsPollingManager = GpsPollingManager.getInstance(context)
            gpsPollingManager.handleResponse(parsedData.isValid)
            
            if (!parsedData.isValid) {
                // Invalid coordinates, we've already handled the retry in the manager
                return
            }
        }
        
        if (userId != -1) {
            // Save battery percentage to SharedPreferences
            parsedData.batteryPercentage?.let { battery ->
                val previousBattery = sharedPreferences.getInt("batteryPercentage", -1)
                
                sharedPreferences.edit()
                    .putInt("batteryPercentage", battery)
                    .putLong("lastBatteryCheck", System.currentTimeMillis())
                    .apply()
                
                // Check if this is a significant battery drop that needs immediate notification
                if (previousBattery > 20 && battery <= 20) {
                    // Trigger immediate battery check
                    val batteryCheckRequest = OneTimeWorkRequestBuilder<BatteryMonitorWorker>()
                        .build()
                    WorkManager.getInstance(context).enqueue(batteryCheckRequest)
                }
            }
            
            // Save GPS data to database if coordinates are available and valid
            if (parsedData.latitude != null && parsedData.longitude != null && parsedData.isValid) {
                val gpsData = GPSData(
                    userId = userId,
                    latitude = parsedData.latitude,
                    longitude = parsedData.longitude,
                    battery = parsedData.batteryPercentage ?: 0,
                    timestamp = System.currentTimeMillis()
                )
                
                // Use coroutine to perform database operation
                CoroutineScope(Dispatchers.IO).launch {
                    val database = AppDatabase.getDatabase(context)
                    val repository = GPSDataRepository(database.gpsDataDao())
                    repository.insertGPSData(gpsData)
                    Log.d("SmsReceiver", "Saved GPS data to database: $gpsData")
                }
            }
        } else {
            Log.e("SmsReceiver", "Cannot save GPS data: No user ID found in SharedPreferences")
        }
    }
}
