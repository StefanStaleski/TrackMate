package com.example.sendsms.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sendsms.components.ActionItem
import com.example.sendsms.components.BaseTemplate
import com.example.sendsms.components.BindResponseDialog
import com.example.sendsms.services.PeriodicSmsWorker
import com.example.sendsms.utils.SMSScheduler
import com.example.sendsms.utils.sendSMS
import com.example.sendsms.viewmodel.ApplicationViewModel
import com.example.sendsms.viewmodel.ApplicationViewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.text.Regex

@Composable
fun ActionsScreen(
    navController: NavHostController,
    applicationViewModel: ApplicationViewModel = viewModel(
        factory = ApplicationViewModelFactory(LocalContext.current.applicationContext as Application)
    ),
    showBindingDialog: Boolean = false,
    bindingMessage: String = "",
    onBindingDialogDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""
    
    // State for bind dialog
    var showBindDialog by remember { mutableStateOf(false) }
    var bindResponseMessage by remember { mutableStateOf("") }
    
    // Get periodic SMS settings
    var periodicSmsEnabled by remember { 
        mutableStateOf(sharedPreferences.getBoolean("periodicSmsEnabled", false)) 
    }
    var periodicSmsMessage by remember { 
        mutableStateOf(sharedPreferences.getString("periodicSmsMessage", "777") ?: "777") 
    }
    
    // Get SMS frequency (default to 60 minutes)
    var smsFrequencyMinutes by remember {
        mutableStateOf(sharedPreferences.getInt("smsFrequencyMinutes", 60))
    }
    
    // Convert minutes to a slider position (0-10)
    val frequencyOptions = listOf(10, 20, 30, 40, 60, 120, 240, 360, 720, 1080, 1440)
    val sliderPosition = remember(smsFrequencyMinutes) {
        frequencyOptions.indexOf(smsFrequencyMinutes).coerceAtLeast(0).toFloat()
    }
    
    val lastSentTimestamp = sharedPreferences.getLong("lastPeriodicSmsSent", 0L)
    val formattedLastSent = if (lastSentTimestamp > 0) {
        val sdf = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(lastSentTimestamp))
    } else {
        "Never"
    }
    
    // Listen for SMS responses that contain binding information
    val receivedSms = sharedPreferences.getString("received_sms", "") ?: ""
    
    LaunchedEffect(receivedSms) {
        if (receivedSms.isNotEmpty()) {
            Log.d("ActionsScreen", "Received SMS: $receivedSms")
            
            if (receivedSms.contains("Set;Binding+")) {
                Log.d("ActionsScreen", "Binding message detected in LaunchedEffect: $receivedSms")
                
                // Extract phone number if available
                val bindingPart = receivedSms.substringAfter("Set;Binding+", "")
                val phoneNumber = if (bindingPart.isNotEmpty()) {
                    // Extract the phone number - it might be followed by other text
                    val phoneNumberPattern = Regex("\\d+")
                    val matchResult = phoneNumberPattern.find(bindingPart)
                    matchResult?.value ?: gpsLocatorNumber
                } else {
                    gpsLocatorNumber
                }
                
                bindResponseMessage = "Phone successfully bound to GPS Locator.\nBound number: $phoneNumber"
                showBindDialog = true
                
                // Clear the received SMS to prevent showing the dialog again
                sharedPreferences.edit().putString("received_sms", "").apply()
            }
        }
    }

    // Load the latest GPS data when the screen opens
    LaunchedEffect(Unit) {
        applicationViewModel.getLatestGPSDataForUser()
    }

    // Update local state based on props
    LaunchedEffect(showBindingDialog, bindingMessage) {
        if (showBindingDialog) {
            showBindDialog = true
            
            // Extract phone number if available
            val phoneNumber = if (bindingMessage.contains("Set;Binding+")) {
                val bindingPart = bindingMessage.substringAfter("Set;Binding+", "")
                if (bindingPart.isNotEmpty()) {
                    // Extract the phone number - it might be followed by other text
                    val phoneNumberPattern = Regex("\\d+")
                    val matchResult = phoneNumberPattern.find(bindingPart)
                    matchResult?.value ?: gpsLocatorNumber
                } else {
                    gpsLocatorNumber
                }
            } else {
                gpsLocatorNumber
            }
            
            bindResponseMessage = "Phone successfully bound to GPS Locator.\nBound number: $phoneNumber"
        }
    }

    BaseTemplate(navController = navController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val iconColor = Color(0xFF4CAF50)

            // Layout for rows with action buttons
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ActionItem(
                        icon = Icons.Filled.LocationOn,
                        text = "Get Location",
                        onClick = {
                            SMSScheduler.scheduleSMS(context, gpsLocatorNumber, "777", 0)
                            applicationViewModel.getLatestGPSDataForUser()
                        },
                        iconColor = iconColor,
                        modifier = Modifier.weight(1f).padding(8.dp)
                    )

                    ActionItem(
                        icon = Icons.Filled.Phone,
                        text = "Call",
                        onClick = {
                            sendSMS(gpsLocatorNumber, "666")
                        },
                        iconColor = iconColor,
                        modifier = Modifier.weight(1f).padding(8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ActionItem(
                        icon = Icons.Filled.Replay,
                        text = "Restart",
                        onClick = {
                            sendSMS(gpsLocatorNumber, "999")
                        },
                        iconColor = iconColor,
                        modifier = Modifier.weight(1f).padding(8.dp)
                    )

                    ActionItem(
                        icon = Icons.Filled.Delete,
                        text = "Delete Memory",
                        onClick = {
                            sendSMS(gpsLocatorNumber, "445")
                        },
                        iconColor = iconColor,
                        modifier = Modifier.weight(1f).padding(8.dp)
                    )
                }
                
                // New row for Bind action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ActionItem(
                        icon = Icons.Filled.Link,
                        text = "Bind",
                        onClick = {
                            Log.d("ActionsScreen", "Sending bind command (000) to $gpsLocatorNumber")
                            sendSMS(gpsLocatorNumber, "000")
                        },
                        iconColor = iconColor,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            // Periodic SMS Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                backgroundColor = Color.DarkGray,
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Automatic Tracking",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Track Automatically",
                            color = Color.White
                        )
                        Switch(
                            checked = periodicSmsEnabled,
                            onCheckedChange = { isEnabled ->
                                periodicSmsEnabled = isEnabled
                                sharedPreferences.edit()
                                    .putBoolean("periodicSmsEnabled", isEnabled)
                                    .apply()
                                // Always use "777" as the message
                                sharedPreferences.edit()
                                    .putString("periodicSmsMessage", "777")
                                    .apply()
                                broadcastSettingsChanged(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4CAF50),
                                checkedTrackColor = Color(0xFF81C784)
                            )
                        )
                    }
                    
                    // Frequency slider
                    if (periodicSmsEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "Tracking Interval: ${formatFrequency(smsFrequencyMinutes)}",
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Slider(
                                value = sliderPosition,
                                onValueChange = { position ->
                                    val index = position.roundToInt().coerceIn(0, frequencyOptions.size - 1)
                                    smsFrequencyMinutes = frequencyOptions[index]
                                    sharedPreferences.edit()
                                        .putInt("smsFrequencyMinutes", smsFrequencyMinutes)
                                        .apply()
                                    broadcastSettingsChanged(context)
                                },
                                valueRange = 0f..10f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4CAF50),
                                    activeTrackColor = Color(0xFF81C784)
                                )
                            )
                            
                            // Show frequency labels
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("10m", color = Color.LightGray, fontSize = 12.sp)
                                Text("1h", color = Color.LightGray, fontSize = 12.sp)
                                Text("4h", color = Color.LightGray, fontSize = 12.sp)
                                Text("12h", color = Color.LightGray, fontSize = 12.sp)
                                Text("24h", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Text(
                        text = "Last tracked: $formattedLastSent",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // Show bind response dialog if needed
        if (showBindDialog) {
            BindResponseDialog(
                message = bindResponseMessage,
                onDismiss = { 
                    showBindDialog = false
                    onBindingDialogDismiss()
                }
            )
        }
    }
}

// Helper function to format the frequency in a human-readable way
private fun formatFrequency(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes minutes"
        minutes == 60 -> "1 hour"
        minutes < 1440 -> "${minutes / 60} hours"
        minutes == 1440 -> "24 hours"
        else -> "${minutes / 60} hours"
    }
}

private fun broadcastSettingsChanged(context: Context) {
    // Just reschedule the worker directly
    PeriodicSmsWorker.schedulePeriodicSms(context)
}


