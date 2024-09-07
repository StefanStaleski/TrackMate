package com.example.sendsms.utils

import android.telephony.SmsManager
import android.util.Log
import com.google.android.gms.maps.model.LatLng

fun sendSMS(phoneNumber: String, message: String) {
    if (phoneNumber.isNotBlank() && message.isNotBlank()) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun isPointInPolygon(point: LatLng, polygon: List<LatLng>): Boolean {
    val x = point.latitude
    val y = point.longitude
    var inside = false
    for (i in polygon.indices) {
        val j = (i + 1) % polygon.size
        val xi = polygon[i].latitude
        val yi = polygon[i].longitude
        val xj = polygon[j].latitude
        val yj = polygon[j].longitude
        val intersect = ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
        if (intersect) inside = !inside
    }

    Log.d("UTILS TAG: ","INSIDE: $inside")
    return inside
}
