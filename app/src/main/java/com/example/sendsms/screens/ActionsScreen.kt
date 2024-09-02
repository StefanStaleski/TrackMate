package com.example.sendsms.screens

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sendsms.components.ActionButton
import com.example.sendsms.components.BaseTemplate
import com.example.sendsms.utils.sendSMS
import com.example.sendsms.viewmodel.ApplicationViewModel
import com.example.sendsms.viewmodel.ApplicationViewModelFactory


@Composable
fun ActionsScreen(navController: NavHostController,
                  applicationViewModel: ApplicationViewModel = viewModel(
                      factory = ApplicationViewModelFactory(LocalContext.current.applicationContext as Application)
                  )
) {

    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    val gpsLocatorNumber = sharedPreferences.getString("gpsLocatorNumber", "") ?: ""

    BaseTemplate(navController = navController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // No background modifier to use the default background
                .padding(16.dp), // Padding around the content
            verticalArrangement = Arrangement.SpaceBetween, // Space between buttons and bottom navigation
            horizontalAlignment = Alignment.CenterHorizontally // Center content horizontally
        ) {
            // Title
            Text(
                text = "Actions Screen",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .padding(bottom = 24.dp) // Space below the title
                    .align(Alignment.CenterHorizontally) // Center title
            )

            // Grid layout for action buttons
            Column(
                modifier = Modifier
                    .weight(1f) // Take up all available space except for the bottom navigation
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Space between rows
            ) {
                // Define action buttons in a grid-like layout
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp) // Space between buttons
                ) {
                    ActionButton(
                        text = "Get Location",
                        onClick = {
                            applicationViewModel.getLatestGPSDataForUser()
                            sendSMS(gpsLocatorNumber, "777")
                        },
                        modifier = Modifier.weight(1f) // Take up equal space
                    )

                    ActionButton(
                        text = "Call",
                        onClick = {
                            sendSMS(gpsLocatorNumber, "CALL!")
                        },
                        modifier = Modifier.weight(1f) // Take up equal space
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp) // Space between buttons
                ) {
                    ActionButton(
                        text = "Delete Memory",
                        onClick = {
                            sendSMS(gpsLocatorNumber, "DELETE MEMORY")
                        },
                        modifier = Modifier.weight(1f) // Take up equal space
                    )

                    ActionButton(
                        text = "Restart",
                        onClick = {
                            sendSMS(gpsLocatorNumber, "RESTART!")
                        },
                        modifier = Modifier.weight(1f) // Take up equal space
                    )
                }
            }
        }
    }

}
