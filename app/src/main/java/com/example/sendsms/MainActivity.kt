package com.example.sendsms

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.sendsms.screens.GoogleMapsScreen
import com.example.sendsms.ui.theme.SendSMSTheme
import com.example.sendsms.ui.theme.GrayToBlackGradient
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import com.example.sendsms.screens.SMSScreen
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sendsms.services.NotificationHelper
import com.example.sendsms.services.NotificationWorker
import java.util.concurrent.TimeUnit
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.sendsms.components.SmsResponseDialog
import com.example.sendsms.utils.DialogUtils
import android.widget.Toast
import com.example.sendsms.screens.SmsDisplayScreen
import com.example.sendsms.services.GpsPollingWorker
import com.example.sendsms.services.GpsTimeoutWorker
import com.example.sendsms.services.BatteryMonitorWorker
import androidx.core.app.ActivityCompat
import com.example.sendsms.services.PeriodicSmsWorker
import com.example.sendsms.screens.SettingsScreen
import com.example.sendsms.SmsReceiver
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.work.OneTimeWorkRequestBuilder
import com.example.sendsms.services.BackgroundService

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
            receivedMessage.value = message
        }
    }

    private val bindingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("message") ?: ""
            Log.d("MainActivity", "Binding message received: $message")
            showBindingDialog.value = true
            bindingMessage.value = message
        }
    }

    private val receivedMessage = mutableStateOf("")
    private val showBindingDialog = mutableStateOf(false)
    private val bindingMessage = mutableStateOf("")

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NotificationWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        // Schedule the GPS polling worker to run every 30 minutes
        val gpsPollingRequest = PeriodicWorkRequestBuilder<GpsPollingWorker>(
            30, TimeUnit.MINUTES,
            15, TimeUnit.MINUTES // Flex period
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "GpsPollingWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            gpsPollingRequest
        )

        // Schedule the timeout checker to run every 30 seconds
        val timeoutCheckRequest = PeriodicWorkRequestBuilder<GpsTimeoutWorker>(
            15, TimeUnit.SECONDS,
            5, TimeUnit.SECONDS // Flex period
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "gps_timeout_check",
            ExistingPeriodicWorkPolicy.REPLACE,
            timeoutCheckRequest
        )

        // Schedule the battery monitor worker to run every hour
        scheduleBatteryMonitorWorker()

        Log.d("MainActivity", "Scheduled GPS polling, timeout, and battery monitor workers")

        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
            .build()
        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest)
        Log.d("MainActivity", "Running NotificationWorker immediately for testing")

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.FOREGROUND_SERVICE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            smsPermissionRequest.launch(permissionsToRequest.toTypedArray())
        }

        // Register receivers with appropriate intent filters
        registerReceiver(smsReceiver, IntentFilter("com.example.sendsms.SMS_RECEIVED"))
        registerReceiver(bindingReceiver, IntentFilter(SmsReceiver.BINDING_ACTION))

        // Initialize notification channels
        NotificationHelper(this).createNotificationChannels()

        setContent {
            SendSMSTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GrayToBlackGradient)
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
                    
                    // Check if user is logged in
                    val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
                    val userId = sharedPreferences.getInt("userId", -1)
                    val username = sharedPreferences.getString("username", null)
                    
                    // Determine start destination based on login status
                    val startDestination = if (isLoggedIn && userId != -1 && username != null) {
                        "profile"
                    } else {
                        // Clear any potentially corrupted login data
                        sharedPreferences.edit()
                            .remove("isLoggedIn")
                            .remove("userId")
                            .remove("username")
                            .apply()
                        "login"
                    }
                    
                    Log.d("MainActivity", "Start destination: $startDestination (isLoggedIn=$isLoggedIn, userId=$userId)")
                    
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
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
                        composable("registration") {
                            RegistrationScreen(
                                navController = navController,
                                onRegister = {
                                    navController.navigate("login") {
                                        popUpTo("registration") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(navController = navController)
                        }
                        composable("actions") {
                            ActionsScreen(
                                navController = navController,
                                showBindingDialog = showBindingDialog.value,
                                bindingMessage = bindingMessage.value,
                                onBindingDialogDismiss = { showBindingDialog.value = false }
                            )
                        }
                        composable("map") {
                            GoogleMapsScreen(navController = navController)
                        }
                        composable("sms") {
                            SMSScreen(
                                navController = navController,
                                receivedMessage = receivedMessage.value
                            )
                        }
                        composable("sms_display") {
                            SmsDisplayScreen(navController = navController)
                        }
                    }
                }
            }
        }

        // Call this in onCreate after requesting other permissions
        requestNotificationPermissionIfNeeded()

        // Schedule workers with immediate execution
        scheduleNotificationWorker()
        scheduleBatteryMonitorWorker()
        scheduleGpsTimeoutWorker()
        schedulePeriodicSmsWorker()

        // Start the background service
        BackgroundService.startService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
        unregisterReceiver(bindingReceiver)
    }

    // Add this function to check if notification permission is granted
    private fun areNotificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    // Call this in onCreate after requesting other permissions
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!areNotificationsEnabled()) {
                Log.d("MainActivity", "Requesting notification permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun scheduleGpsTimeoutWorker() {
        val timeoutCheckRequest = PeriodicWorkRequestBuilder<GpsTimeoutWorker>(
            15, TimeUnit.SECONDS,
            5, TimeUnit.SECONDS // Flex period
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "gps_timeout_check",
            ExistingPeriodicWorkPolicy.REPLACE,
            timeoutCheckRequest
        )
        
        Log.d("MainActivity", "Scheduled GpsTimeoutWorker to run every 15 seconds")
    }

    private fun schedulePeriodicSmsWorker() {
        PeriodicSmsWorker.schedulePeriodicSms(this)
    }

    private fun scheduleNotificationWorker() {
        val notificationWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            15, TimeUnit.MINUTES
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "notification_worker",
            ExistingPeriodicWorkPolicy.UPDATE,
            notificationWorkRequest
        )
        
        // Also run it once immediately
        val oneTimeRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
        WorkManager.getInstance(this).enqueue(oneTimeRequest)
        
        Log.d("MainActivity", "Scheduled NotificationWorker to run every 15 minutes")
    }

    private fun scheduleBatteryMonitorWorker() {
        val batteryWorkRequest = PeriodicWorkRequestBuilder<BatteryMonitorWorker>(
            15, TimeUnit.MINUTES
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "battery_monitor",
            ExistingPeriodicWorkPolicy.UPDATE,
            batteryWorkRequest
        )
        
        // Also run it once immediately
        val oneTimeRequest = OneTimeWorkRequestBuilder<BatteryMonitorWorker>().build()
        WorkManager.getInstance(this).enqueue(oneTimeRequest)
        
        Log.d("MainActivity", "Scheduled BatteryMonitorWorker to run every 15 minutes")
    }
}
