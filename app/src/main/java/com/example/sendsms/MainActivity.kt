package com.example.sendsms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sendsms.screens.LoginScreen
import com.example.sendsms.screens.RegistrationScreen
import com.example.sendsms.screens.ProfileScreen // Import ProfileScreen
import com.example.sendsms.screens.ActionsScreen
import com.example.sendsms.screens.SettingsScreen
import com.example.sendsms.ui.components.BottomNavigation
import com.example.sendsms.ui.theme.SendSMSTheme
import android.util.Log
import com.example.sendsms.screens.SMSScreen

class MainActivity : ComponentActivity() {
    private val smsPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.SEND_SMS] == true && permissions[Manifest.permission.RECEIVE_SMS] == true) {
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

    // State to track login status
    private var isLoggedIn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request SMS permissions
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }
        if (permissionsToRequest.isNotEmpty()) {
            smsPermissionRequest.launch(permissionsToRequest.toTypedArray())
        }

        // Register the BroadcastReceiver to listen for local broadcasts
        val intentFilter = IntentFilter("com.example.sendsms.SMS_RECEIVED")
        registerReceiver(smsReceiver, intentFilter)

        // Set the content of the activity
        setContent {
            SendSMSTheme {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (isLoggedIn) {
                            BottomNavigation(navController)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) "profile" else "login", // Update the start destination
                        Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                navController = navController,
                                onLogin = { username, password ->
                                    // Add login logic here, e.g., validate user
                                    isLoggedIn = true // Set login state to true
                                    navController.navigate("profile") // Navigate to Profile screen after login
                                }
                            )
                        }
                        composable("register") {
                            RegistrationScreen(
                                onRegister = { username, password, locatorNumber ->
                                    navController.navigateUp() // Navigate back to login screen
                                }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                navController = navController // Pass navController to ProfileScreen
                            )
                        }
                        composable("actions") {
                            ActionsScreen(navController)
                        }
                        composable("settings") {
                            SettingsScreen(navController)
                        }
                        composable("sms") { // Add this route
                            SMSScreen(
                                navController = navController,
                                receivedMessage = receivedMessage.value
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }
}
