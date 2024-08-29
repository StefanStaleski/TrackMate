package com.example.sendsms

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sendsms.screens.LoginScreen
import com.example.sendsms.screens.RegistrationScreen
import com.example.sendsms.screens.ProfileScreen
import com.example.sendsms.screens.ActionsScreen
import com.example.sendsms.screens.SettingsScreen
import com.example.sendsms.ui.theme.SendSMSTheme
import com.example.sendsms.ui.theme.GrayToBlackGradient
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import com.example.sendsms.screens.SMSScreen

class MainActivity : ComponentActivity() {
    private val smsPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
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
    private val receivedMessage: State<String> get() = _receivedMessage

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }
        if (permissionsToRequest.isNotEmpty()) {
            smsPermissionRequest.launch(permissionsToRequest.toTypedArray())
        }

        val intentFilter = IntentFilter("com.example.sendsms.SMS_RECEIVED")
        registerReceiver(smsReceiver, intentFilter)

        setContent {
            SendSMSTheme {
                val navController = rememberNavController()

                val context = LocalContext.current
                val sharedPreferences =
                    context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
                val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GrayToBlackGradient)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) "profile" else "login",
                    ) {
                        composable("login") {
                            LoginScreen(
                                navController = navController,
                                onLogin = {
                                    navController.navigate("profile") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("register") {
                            RegistrationScreen(
                                navController = navController,
                                onRegister = {
                                }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                navController = navController
                            )
                        }
                        composable("actions") {
                            ActionsScreen(navController)
                        }
                        composable("settings") {
                            SettingsScreen(navController)
                        }
                        composable("sms") {
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
