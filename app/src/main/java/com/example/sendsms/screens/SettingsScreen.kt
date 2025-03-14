package com.example.sendsms.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.sendsms.components.BaseTemplate
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    
    // Get current settings
    var periodicSmsEnabled by remember { 
        mutableStateOf(sharedPreferences.getBoolean("periodicSmsEnabled", false)) 
    }
    var periodicSmsMessage by remember { 
        mutableStateOf(sharedPreferences.getString("periodicSmsMessage", "777") ?: "777") 
    }
    
    val lastSentTimestamp = sharedPreferences.getLong("lastPeriodicSmsSent", 0L)
    val formattedLastSent = if (lastSentTimestamp > 0) {
        val sdf = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(lastSentTimestamp))
    } else {
        "Never"
    }
    
    BaseTemplate(navController = navController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.DarkGray
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Periodic SMS Settings",
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
                            text = "Send SMS hourly",
                            color = Color.White
                        )
                        Switch(
                            checked = periodicSmsEnabled,
                            onCheckedChange = { isEnabled ->
                                periodicSmsEnabled = isEnabled
                                sharedPreferences.edit()
                                    .putBoolean("periodicSmsEnabled", isEnabled)
                                    .apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4CAF50),
                                checkedTrackColor = Color(0xFF81C784)
                            )
                        )
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF4CAF50),
                            unfocusedLabelColor = Color.Gray,
                            cursorColor = Color(0xFF4CAF50),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
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