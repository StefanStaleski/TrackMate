package com.example.sendsms.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.sendsms.components.BaseTemplate

@Composable
fun SettingsScreen(navController: NavHostController) {
    BaseTemplate(navController = navController) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Settings Screen", style = MaterialTheme.typography.headlineLarge)
        }
    }
}
