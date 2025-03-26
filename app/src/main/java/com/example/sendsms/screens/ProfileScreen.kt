package com.example.sendsms.screens

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.sendsms.components.AppButton
import com.example.sendsms.components.EditProfileDialog
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sendsms.components.BaseTemplate
import com.example.sendsms.viewmodel.ApplicationViewModel
import com.example.sendsms.viewmodel.ApplicationViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.sendsms.components.FirstTimeInfoDialog

@Composable
fun ProfileScreen(
    navController: NavHostController,
    applicationViewModel: ApplicationViewModel = viewModel(
    factory = ApplicationViewModelFactory(LocalContext.current.applicationContext as Application)
)) {

    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

    var isFirstTimeLogin by remember { 
        mutableStateOf(sharedPreferences.getBoolean("isFirstTimeLogin", true)) 
    }

    val userName by remember {
        mutableStateOf(
            sharedPreferences.getString("username", "N/A") ?: "N/A"
        )
    }

    val gpsLocatorNumber by remember {
        mutableStateOf(
            sharedPreferences.getString(
                "gpsLocatorNumber",
                "N/A"
            ) ?: "N/A"
        )
    }

    val batteryPercentage by remember {
        mutableStateOf(
            sharedPreferences.getInt("batteryPercentage", 0)
                .takeIf { it > 0 }
                ?.let { "$it%" }
                ?: "N/A"
        )
    }

    val lastBatteryCheckTimestamp by remember {
        mutableLongStateOf(
            sharedPreferences.getLong("lastBatteryCheck", 0L)
        )
    }

    val formattedLastCheck = if (lastBatteryCheckTimestamp > 0) {
        val sdf = SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(lastBatteryCheckTimestamp))
    } else {
        "N/A"
    }


    var showDialog by remember { mutableStateOf(false) }

    fun handleSave(newUsername: String, newGpsLocator: String) {
        val currentUsername = sharedPreferences.getString("username", null) ?: return

        applicationViewModel.updateUser(
            username = currentUsername,
            newUsername = newUsername,
            newPassword = null,
            newGpsLocatorNumber = newGpsLocator
        )

        with(sharedPreferences.edit()) {
            putString("username", newUsername)
            putString("gpsLocatorNumber", newGpsLocator)
            apply()
        }
    }

    LaunchedEffect(sharedPreferences) {
        Log.d("ProfileScreen", "Username: $userName")
        Log.d("ProfileScreen", "GPS Locator Number: $gpsLocatorNumber")
    }
    BaseTemplate(navController = navController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                elevation = CardDefaults.elevatedCardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Username Icon",
                        tint = Color.White, // Icon color
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Username: $userName",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        fontSize = 16.sp
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                elevation = CardDefaults.elevatedCardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "GPS Icon",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "GPS Locator Number: $gpsLocatorNumber",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        fontSize = 16.sp
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                elevation = CardDefaults.elevatedCardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = "Battery Icon",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Current Battery Percentage: $batteryPercentage",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        fontSize = 16.sp
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                elevation = CardDefaults.elevatedCardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time Icon",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Last Battery Check: $formattedLastCheck",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        fontSize = 16.sp
                    )
                }
            }
            AppButton(
                text = "Edit Profile",
                onClick = { showDialog = true },
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            AppButton(
                text = "Logout",
                onClick = { 
                    // Clear ALL login-related data
                    with(sharedPreferences.edit()) {
                        putBoolean("isLoggedIn", false)
                        remove("userId")
                        remove("username")
                        remove("gpsLocatorNumber")
                        // You might want to keep some settings like notification preferences
                        apply()
                    }
                    // Navigate back to login screen
                    navController.navigate("login") {
                        popUpTo("profile") { inclusive = true }
                    }
                },
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        EditProfileDialog(
            showDialog = showDialog,
            onDismiss = { showDialog = false },
            onSave = { newUserName, newGpsLocator ->
                handleSave(newUserName, newGpsLocator)
            },
            currentUserName = userName,
            currentGpsLocator = gpsLocatorNumber
        )

        if (isFirstTimeLogin) {
            FirstTimeInfoDialog(
                onDismiss = {
                    // Mark first time login as completed
                    sharedPreferences.edit()
                        .putBoolean("isFirstTimeLogin", false)
                        .apply()
                    isFirstTimeLogin = false
                }
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(navController = rememberNavController())
}
