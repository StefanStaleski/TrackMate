package com.example.sendsms.utils

import android.util.Log
import java.util.regex.Pattern

object SmsParser {
    private val BATTERY_PATTERN = Pattern.compile("VBT:(\\d+)%")
    private val COORDINATES_PATTERN = Pattern.compile("q=([-+]?\\d*\\.?\\d+),([-+]?\\d*\\.?\\d+)")

    data class SmsData(
        val batteryPercentage: Int? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val isValid: Boolean = true
    )

    fun parseGpsLocatorSms(message: String): SmsData {
        Log.d("SmsParser", "Parsing message: $message")
        
        // Extract battery percentage
        val batteryMatcher = BATTERY_PATTERN.matcher(message)
        val batteryPercentage = if (batteryMatcher.find()) {
            batteryMatcher.group(1)?.toIntOrNull()
        } else null
        
        // Extract coordinates
        val coordinatesMatcher = COORDINATES_PATTERN.matcher(message)
        val latitude = if (coordinatesMatcher.find()) {
            coordinatesMatcher.group(1)?.toDoubleOrNull()
        } else null
        
        val longitude = if (latitude != null) {
            coordinatesMatcher.group(2)?.toDoubleOrNull()
        } else null
        
        // Check if coordinates are valid (not -1,-1)
        val isValid = !(latitude == -1.0 && longitude == -1.0)
        
        val result = SmsData(batteryPercentage, latitude, longitude, isValid)
        Log.d("SmsParser", "Parsed data: $result")
        return result
    }
} 