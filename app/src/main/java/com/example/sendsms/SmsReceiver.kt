package com.example.sendsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sendsms.database.AppDatabase
import com.example.sendsms.database.entity.GPSData
import com.example.sendsms.database.repository.GPSDataRepository
import com.example.sendsms.services.BatteryMonitorWorker
import com.example.sendsms.services.NotificationHelper
import com.example.sendsms.utils.DialogUtils
import com.example.sendsms.utils.SmsParser
import com.example.sendsms.utils.GpsPollingManager
import com.example.sendsms.utils.SMSScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        Log.d("SmsReceiver", "Processing message: $message")
        val parsedData = SmsParser.parseGpsLocatorSms(message)
        Log.d("SmsReceiver", "Parsed data: lat=${parsedData.latitude}, lng=${parsedData.longitude}, isSpecificallyInvalid=${parsedData.isSpecificallyInvalid}")
        
        // Get user ID from SharedPreferences
        val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)
        val pollingInProgress = sharedPreferences.getBoolean("gpsPollingInProgress", false)
        
        Log.d("SmsReceiver", "Processed message: isValid=${parsedData.isValid}, isSpecificallyInvalid=${parsedData.isSpecificallyInvalid}")
        
        // Check for specifically invalid coordinates (-1,-1)
        if (parsedData.isSpecificallyInvalid) {
            // Track consecutive invalid responses
            val consecutiveInvalidCount = sharedPreferences.getInt("consecutiveInvalidCount", 0) + 1
            sharedPreferences.edit()
                .putInt("consecutiveInvalidCount", consecutiveInvalidCount)
                .putString("lastGpsResponseType", "specifically_invalid")
                .apply()
            
            Log.d("SmsReceiver", "Received specifically invalid coordinates (-1,-1). Count: $consecutiveInvalidCount")
            
            if (consecutiveInvalidCount >= 3) {
                // After 3 consecutive invalid responses, send notification
                val notificationHelper = NotificationHelper(context)
                notificationHelper.sendNotification(
                    "GPS Locator Error",
                    "GPS Locator is sending invalid data (-1,-1) after multiple attempts. Please check the device.",
                    NotificationHelper.CHANNEL_GPS_ERROR
                )
                
                // Reset counter after notification
                sharedPreferences.edit()
                    .putInt("consecutiveInvalidCount", 0)
                    .putLong("lastInvalidNotification", System.currentTimeMillis())
                    .apply()
            } else {
                // Immediately retry with another SMS
                val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""
                if (gpsLocatorNumber.isNotBlank()) {
                    Log.d("SmsReceiver", "Automatically retrying after receiving invalid coordinates")
                    // Make sure we're using milliseconds for the delay
                    SMSScheduler.scheduleSMS(context, gpsLocatorNumber, "777", 2000) // 2-second delay before retry
                }
            }
            
            return // Don't process this as a valid response
        } else {
            // Reset consecutive invalid counter if we got a valid or different type of invalid response
            sharedPreferences.edit().putInt("consecutiveInvalidCount", 0).apply()
        }
        
        // Update polling status in SharedPreferences
        if (pollingInProgress || sharedPreferences.getBoolean("manualGpsPollingInProgress", false)) {
            // Notify the polling manager about the response
            val gpsPollingManager = GpsPollingManager.getInstance(context)
            gpsPollingManager.handleResponse(parsedData.isValid, parsedData.isSpecificallyInvalid)
            
            // Also reset manual polling flag if this was a manual poll
            if (sharedPreferences.getBoolean("manualGpsPollingInProgress", false)) {
                sharedPreferences.edit()
                    .putBoolean("manualGpsPollingInProgress", false)
                    .apply()
            }
            
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
