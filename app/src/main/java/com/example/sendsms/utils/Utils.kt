package com.example.sendsms.utils

import android.telephony.SmsManager

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
