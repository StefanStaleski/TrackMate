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
            val sharedPreferences =
                context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            sharedPreferences.edit().putString("received_sms", message).apply()
            // Trigger recomposition
            _receivedMessage.value = message    
        }
    }

    private val _receivedMessage = mutableStateOf("")
    private val receivedMessage: State<String> get() = _receivedMessage

    private val dialogReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("DialogReceiver", "Received intent: ${intent.action}")
            if (intent.action == DialogUtils.ACTION_SHOW_SMS_DIALOG) {
                val message = intent.getStringExtra(DialogUtils.EXTRA_SMS_MESSAGE) ?: ""
                val sender = intent.getStringExtra(DialogUtils.EXTRA_SMS_SENDER) ?: ""
                
                Log.d("DialogReceiver", "Showing dialog for message: $message from $sender")
                
                // Update dialog state
                _showSmsDialog.value = true
                _dialogSender.value = sender
                _dialogMessage.value = message
                
                // Also show a Toast as a fallback
                Toast.makeText(
                    context,
                    "GPS Locator: $message",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // State for dialog
    private val _showSmsDialog = mutableStateOf(false)
    private val _dialogSender = mutableStateOf("")
    private val _dialogMessage = mutableStateOf("")

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
        val batteryMonitorRequest = PeriodicWorkRequestBuilder<BatteryMonitorWorker>(
            1, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // Flex period
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BatteryMonitorWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            batteryMonitorRequest
        )

        // Schedule the periodic SMS worker to run every hour
        schedulePeriodicSmsWorker()

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

        if (permissionsToRequest.isNotEmpty()) {
            smsPermissionRequest.launch(permissionsToRequest.toTypedArray())
        }

        val intentFilter = IntentFilter("com.example.sendsms.SMS_RECEIVED")
        registerReceiver(smsReceiver, intentFilter)

        // Register for dialog broadcasts
        val dialogIntentFilter = IntentFilter(DialogUtils.ACTION_SHOW_SMS_DIALOG)
        LocalBroadcastManager.getInstance(this).registerReceiver(dialogReceiver, dialogIntentFilter)

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
                        startDestination = "profile"
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
                            ActionsScreen(navController = navController)
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

                // Add the dialog
                if (_showSmsDialog.value) {
                    SmsResponseDialog(
                        sender = _dialogSender.value,
                        message = _dialogMessage.value,
                        onDismiss = { _showSmsDialog.value = false }
                    )
                }
            }
        }

        // Call this in onCreate after requesting other permissions
        requestNotificationPermissionIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dialogReceiver)
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
}
