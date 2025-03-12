package com.example.sendsms.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SmsResponseDialog(
    sender: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "GPS Locator Response",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        },
        text = {
            Text("Message from $sender:\n\n$message")
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("OK")
            }
        }
    )
} 