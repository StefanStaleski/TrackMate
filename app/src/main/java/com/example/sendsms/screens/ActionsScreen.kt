package com.example.sendsms.screens

import android.app.Application
import android.content.Context
import android.content.Intent
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
import com.example.sendsms.services.PeriodicSmsWorker
import com.example.sendsms.utils.SMSScheduler
import com.example.sendsms.utils.sendSMS
import com.example.sendsms.viewmodel.ApplicationViewModel
import com.example.sendsms.viewmodel.ApplicationViewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ActionsScreen(
    navController: NavHostController,
    applicationViewModel: ApplicationViewModel = viewModel(
        factory = ApplicationViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""

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

    // Load the latest GPS data when the screen opens
    LaunchedEffect(Unit) {
        applicationViewModel.getLatestGPSDataForUser()
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

            // Layout for two rows with two buttons each
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
                            sendSMS(gpsLocatorNumber, "CALL!")
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
                        icon = Icons.Filled.Mic,
                        text = "Listen",
                        onClick = {
                            sendSMS(gpsLocatorNumber, "MONITOR!")
                        },
                        iconColor = iconColor,
                        modifier = Modifier.weight(1f).padding(8.dp)
                    )

                    ActionItem(
                        icon = Icons.Filled.BatteryAlert,
                        text = "Battery",
                        onClick = {
                            sendSMS(gpsLocatorNumber, "BAT!")
                        },
                        iconColor = iconColor,
                        modifier = Modifier.weight(1f).padding(8.dp)
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
                        text = "Automatic SMS Settings",
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
                            text = "Send SMS Automatically",
                            color = Color.White
                        )
                        Switch(
                            checked = periodicSmsEnabled,
                            onCheckedChange = { isEnabled ->
                                periodicSmsEnabled = isEnabled
                                sharedPreferences.edit()
                                    .putBoolean("periodicSmsEnabled", isEnabled)
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
                                text = "Frequency: ${formatFrequency(smsFrequencyMinutes)}",
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
                    
                    OutlinedTextField(
                        value = periodicSmsMessage,
                        onValueChange = { message ->
                            periodicSmsMessage = message
                            sharedPreferences.edit()
                                .putString("periodicSmsMessage", message)
                                .apply()
                        },
                        label = { Text("SMS Message") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF4CAF50),
                            unfocusedLabelColor = Color.Gray,
                            cursorColor = Color(0xFF4CAF50),
                            textColor = Color.White
                        )
                    )
                    
                    Text(
                        text = "Last sent: $formattedLastSent",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }
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


