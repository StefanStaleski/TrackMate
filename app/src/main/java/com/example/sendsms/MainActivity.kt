package com.example.sendsms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sendsms.ui.theme.SendSMSTheme
import androidx.compose.ui.platform.LocalContext
import android.util.Log

class MainActivity : ComponentActivity() {
    private val smsPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            Log.d("MainActivity", "Permissions GRANTED")
        } else {
            Log.d("MainActivity", "Permissions DENIED")
        }
    }

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("message") ?: ""
            // Update the received message in SharedPreferences and the UI
            val sharedPreferences = context.getSharedPreferences("SMS_PREFS", Context.MODE_PRIVATE)
            sharedPreferences.edit().putString("received_sms", message).apply()
            // Trigger recomposition
            _receivedMessage.value = message
        }
    }

    private val _receivedMessage = mutableStateOf("")
    val receivedMessage: State<String> get() = _receivedMessage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request SMS permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            smsPermissionRequest.launch(Manifest.permission.SEND_SMS)
            smsPermissionRequest.launch(Manifest.permission.RECEIVE_SMS)
        }

        // Register the BroadcastReceiver to listen for local broadcasts
        val intentFilter = IntentFilter("com.example.sendsms.SMS_RECEIVED")
        registerReceiver(smsReceiver, intentFilter)

        // Set the content of the activity
        setContent {
            SendSMSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SMSApp(
                        modifier = Modifier.padding(innerPadding),
                        receivedMessage = receivedMessage.value
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }
}

@Composable
fun SMSApp(modifier: Modifier = Modifier, receivedMessage: String) {
    var phoneNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                sendSMS(phoneNumber, message)
                status = "SMS sent!"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send SMS")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Received SMS: $receivedMessage")
    }
}

private fun sendSMS(phoneNumber: String, message: String) {
    if (phoneNumber.isNotBlank() && message.isNotBlank()) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
