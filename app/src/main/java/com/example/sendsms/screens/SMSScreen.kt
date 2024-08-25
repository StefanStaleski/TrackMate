package com.example.sendsms.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.sendsms.utils.sendSMS

@Composable
fun SMSScreen(
    navController: NavHostController,
    receivedMessage: String
) {
    var phoneNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
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
        Button(
            onClick = {
                navController.navigate("registration")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Registration")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = status)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Received SMS: $receivedMessage")
    }
}
