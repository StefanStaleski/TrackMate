package com.example.sendsms.screens

import android.app.Application
import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sendsms.components.ActionItem
import com.example.sendsms.components.BaseTemplate
import com.example.sendsms.utils.sendSMS
import com.example.sendsms.viewmodel.ApplicationViewModel
import com.example.sendsms.viewmodel.ApplicationViewModelFactory

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

    BaseTemplate(navController = navController) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Add padding around the content
            verticalArrangement = Arrangement.SpaceBetween,
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
                            applicationViewModel.getLatestGPSDataForUser()
                            sendSMS(gpsLocatorNumber, "777")
                        },
                        iconColor = iconColor,
                        modifier = Modifier.weight(1f).padding(8.dp) // Weight to fill space evenly
                    )

                    ActionItem(
                        icon = Icons.Filled.Phone,
                        text = "Call",
                        onClick = {
                            sendSMS(gpsLocatorNumber, "CALL!")
                        },
                        iconColor = iconColor,
                        modifier = Modifier.weight(1f).padding(8.dp) // Weight to fill space evenly
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
                            sendSMS(gpsLocatorNumber, "RESTART!")
                        },
                        iconColor = iconColor,
                        modifier = Modifier.weight(1f).padding(8.dp) // Weight to fill space evenly
                    )
                    ActionItem(
                        icon = Icons.Filled.Delete,
                        text = "Delete Memory",
                        onClick = {
                            sendSMS(gpsLocatorNumber, "DELETE MEMORY")
                        },
                        iconColor = iconColor,
                        modifier = Modifier.weight(1f).padding(8.dp) // Weight to fill space evenly
                    )
                }
            }
        }
    }
}


