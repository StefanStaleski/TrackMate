package com.example.sendsms.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavHostController
import com.example.sendsms.components.ActionButton

@Composable
fun ActionsScreen(navController: NavHostController) {
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
                    onClick = { /* Handle Get Location */ },
                    modifier = Modifier.weight(1f) // Take up equal space
                )

                ActionButton(
                    text = "Call",
                    onClick = { /* Handle Call */ },
                    modifier = Modifier.weight(1f) // Take up equal space
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp) // Space between buttons
            ) {
                ActionButton(
                    text = "Delete Memory",
                    onClick = { /* Handle Delete Memory */ },
                    modifier = Modifier.weight(1f) // Take up equal space
                )

                ActionButton(
                    text = "Restart",
                    onClick = { /* Handle Restart */ },
                    modifier = Modifier.weight(1f) // Take up equal space
                )
            }
        }
    }
}
