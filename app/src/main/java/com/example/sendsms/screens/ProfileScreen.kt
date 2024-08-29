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
import com.example.sendsms.viewmodel.UserViewModel
import com.example.sendsms.viewmodel.UserViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfileScreen(
    navController: NavHostController,
    userViewModel: UserViewModel = viewModel(
    factory = UserViewModelFactory(LocalContext.current.applicationContext as Application)
),) {

    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

    // Example user data
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
    val currentBatteryPercentage = "85%"

    // Dialog state
    var showDialog by remember { mutableStateOf(false) }

    // Example last battery check time using Calendar
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR_OF_DAY, -3) // Example data: 3 hours ago

    // Format last battery check time
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val formattedLastCheck = sdf.format(calendar.time)

    // Handle save action
    fun handleSave(newUsername: String, newGpsLocator: String) {
        val currentUsername = sharedPreferences.getString("username", null) ?: return

        userViewModel.updateUser(
            username = currentUsername,
            newUsername = newUsername,
            newPassword = null, // Assuming password is not being changed here
            newGpsLocatorNumber = newGpsLocator
        )

        with(sharedPreferences.edit()) {
            putString("username", newUsername)
            putString("gpsLocatorNumber", newGpsLocator)
            apply() // or commit() if you prefer
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
                .padding(16.dp), // Padding around the entire column
            verticalArrangement = Arrangement.Center, // Center content vertically
            horizontalAlignment = Alignment.CenterHorizontally // Center content horizontally
        ) {
            // User Name
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray), // Card background color
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
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White), // Text color
                        fontSize = 16.sp
                    )
                }
            }

            // GPS Locator Number
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray), // Card background color
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
                        tint = Color.White, // Icon color
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "GPS Locator Number: $gpsLocatorNumber",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White), // Text color
                        fontSize = 16.sp
                    )
                }
            }

            // Current Battery Percentage
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray), // Card background color
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
                        tint = Color.White, // Icon color
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Current Battery Percentage: $currentBatteryPercentage",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White), // Text color
                        fontSize = 16.sp
                    )
                }
            }

            // Last Battery Check
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray), // Card background color
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
                        tint = Color.White, // Icon color
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Last Battery Check: $formattedLastCheck",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White), // Text color
                        fontSize = 16.sp
                    )
                }
            }

            // Edit Profile Button
            AppButton(
                text = "Edit Profile",
                onClick = { showDialog = true },
                backgroundColor = Color.Blue, // Button background color
                contentColor = Color.White, // Button text color
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Navigate to SMS Screen Button
            AppButton(
                text = "Send SMS",
                onClick = { navController.navigate("sms") }, // Navigate to the SMSScreen
                backgroundColor = Color.Blue, // Button background color
                contentColor = Color.White, // Button text color
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        // EditProfileDialog
        EditProfileDialog(
            showDialog = showDialog,
            onDismiss = { showDialog = false },
            onSave = { newUserName, newGpsLocator ->
                handleSave(newUserName, newGpsLocator)
            },
            currentUserName = userName,
            currentGpsLocator = gpsLocatorNumber
        )
    }
}


@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    // Provide a dummy NavHostController for preview
    ProfileScreen(navController = rememberNavController())
}
