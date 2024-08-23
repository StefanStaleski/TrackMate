package com.example.sendsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TARGET_SENDER = "+38975523294"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle["pdus"] as Array<Any>?
                if (pdus != null) {
                    for (pdu in pdus) {
                        val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                        val messageBody = smsMessage.messageBody
                        val sender = smsMessage.displayOriginatingAddress

                        Log.d("SmsReceiver", "Received SMS from: $sender with message: $messageBody")

                        // Save the received message to SharedPreferences
                        val sharedPreferences = context.getSharedPreferences("SMS_PREFS", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        val message = if (sender == TARGET_SENDER) {
                            "From: $sender\nMessage: $messageBody"
                        } else {
                            "From: $sender\nMessage: $messageBody (Ignored)"
                        }
                        editor.putString("received_sms", message)
                        editor.apply()

                        // Optionally, send a local broadcast or update the UI directly
                        val localIntent = Intent("com.example.sendsms.SMS_RECEIVED")
                        localIntent.putExtra("message", message)
                        context.sendBroadcast(localIntent)
                    }
                }
            }
        }
    }
}