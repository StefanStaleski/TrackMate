package com.example.sendsms.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun BindResponseDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Binding Complete",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        },
        text = {
            Text(message)
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