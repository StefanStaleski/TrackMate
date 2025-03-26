package com.example.sendsms.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FirstTimeInfoDialog(
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    var canDismiss by remember { mutableStateOf(false) }
    
    // Enable dismiss button when scrolled to bottom or after a delay
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            // Listen for scroll position changes
            snapshotFlow { scrollState.value }
                .collect { scrollPosition ->
                    // Enable button when scrolled to 90% of content or more
                    if (scrollPosition >= scrollState.maxValue * 0.9f) {
                        canDismiss = true
                    }
                }
        } else {
            // If there's no scrollable content, enable the button
            canDismiss = true
        }
    }

    AlertDialog(
        onDismissRequest = { /* Prevent dismiss on outside click */ },
        containerColor = Color(0xFF333333),
        title = {
            Text(
                text = "Welcome to TrackMate",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Important Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Before starting to use this application, please make sure that you have thoroughly read the GF 07 Mini GPS Locator Manual.",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "Getting Started:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "1. First, bind your phone number with the GPS Locator by clicking the \"Bind\" button in the Actions screen.\n\n" +
                           "2. Wait a few seconds up to a minute to receive a confirmation through a window pop-up.\n\n" +
                           "3. After binding is confirmed, you can track manually or set up automatic tracking using the slider button.",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "Permissions Required:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "• SMS: Required to communicate with your GPS locator device\n\n" +
                           "• Notifications: To keep you informed about tracking status and device alerts\n\n" +
                           "• Background processing: To enable automatic tracking even when the app is closed\n\n" +
                           "• Location: Required to display your current position on the map alongside your GPS locator's position, allowing you to see relative distances and directions. This helps you navigate to your tracked device more effectively.",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "Notifications You'll Receive:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "• Battery Alerts: Notifications when your GPS locator's battery level is low\n\n" +
                           "• Location Updates: Notifications when new location data is received from your device\n\n" +
                           "• GPS Error Alerts: Notifications if there are issues with GPS tracking or timeouts\n\n" +
                           "• Boundary Alerts: Notifications when your device exits or is close to exiting the defined geographic areas",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "Important Notes:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "• Please do not log out if you want the application to continue tracking in the background.\n\n" +
                           "• Automatic tracking will send periodic requests to your GPS locator device based on your selected interval.\n\n" +
                           "• Battery information is updated each time location is requested.\n\n" +
                           "• You can create geofence boundaries on the map to monitor when your device exits or is close to exiting the specific areas.",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "Battery Optimization:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "To maximize battery life on both your phone and GPS locator device, TrackMate uses intelligent scheduling. Please note that automatic tracking intervals may vary slightly from the exact times selected. For example, if you set a 10-minute tracking interval, the actual time between updates might sometimes be longer, sometimes shorter. This slight variation helps optimize system resources and extend battery life while maintaining reliable tracking functionality.",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "By continuing to use this application, you acknowledge that you have read and understood this information.",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                enabled = canDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canDismiss) Color(0xFF4CAF50) else Color.Gray,
                    contentColor = Color.White
                )
            ) {
                Text("I Understand")
            }
        },
        dismissButton = null
    )
} 