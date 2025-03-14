package com.example.sendsms.utils

import android.util.Log

data class GpsData(
    val latitude: Double?,
    val longitude: Double?,
    val batteryPercentage: Int?,
    val isValid: Boolean,
    val isSpecificallyInvalid: Boolean = false // Flag for -1,-1 coordinates
)

object SmsParser {
    private const val TAG = "SmsParser"

    fun parseGpsLocatorSms(message: String): GpsData {
        Log.d(TAG, "Parsing GPS message: $message")
        
        try {
            // Extract battery percentage - updated regex for "VBT:19%," format
            val batteryRegex = """VBT:(\d+)%""".toRegex()
            val batteryMatch = batteryRegex.find(message)
            var battery = batteryMatch?.groupValues?.get(1)?.toIntOrNull()
            
            // Fallback battery extraction if the first pattern fails
            if (battery == null) {
                val fallbackBatteryRegex = """VBT[:\s](\d+)%""".toRegex()
                val fallbackMatch = fallbackBatteryRegex.find(message)
                battery = fallbackMatch?.groupValues?.get(1)?.toIntOrNull()
                Log.d(TAG, "Fallback battery extraction: match=${fallbackMatch?.value}, battery=$battery")
            }
            
            Log.d(TAG, "Battery extraction: match=${batteryMatch?.value}, battery=$battery")
            
            // Extract coordinates from URL - updated for possible space after comma
            val coordsRegex = """maps\?q=(-?\d+\.?\d*),\s*(-?\d+\.?\d*)""".toRegex()
            val coordsMatch = coordsRegex.find(message)
            
            if (coordsMatch != null && coordsMatch.groupValues.size >= 3) {
                val lat = coordsMatch.groupValues[1].toDoubleOrNull()
                val lng = coordsMatch.groupValues[2].toDoubleOrNull()
                
                // Check if coordinates are specifically -1,-1 (invalid GPS data indicator)
                val isSpecificallyInvalid = (lat == -1.0 && lng == -1.0)
                
                Log.d(TAG, "Extracted coordinates: lat=$lat, lng=$lng, isSpecificallyInvalid=$isSpecificallyInvalid")
                
                // Check if coordinates are valid (not null and within range)
                val isValid = lat != null && lng != null && 
                              lat >= -90.0 && lat <= 90.0 && 
                              lng >= -180.0 && lng <= 180.0 &&
                              !isSpecificallyInvalid
                
                return GpsData(lat, lng, battery, isValid, isSpecificallyInvalid)
            } else {
                Log.d(TAG, "No coordinates found in message")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GPS message: ${e.message}", e)
        }
        
        return GpsData(null, null, null, false)
    }
} 