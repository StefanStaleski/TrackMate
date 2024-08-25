package com.example.sendsms.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person

@Composable
fun ProfileScreen(navController: NavHostController) {
    // Example user data
    val userName = "John Doe"
    val userEmail = "john.doe@example.com"

    // Content of the Profile Screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture
        Surface(
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(60.dp),
            color = Color.Gray
        ) {
            // Placeholder for profile picture
            Icon(
                imageVector = Icons.Default.Person, // Use a default person icon
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                tint = Color.White
            )
        }

        // User Name
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 24.sp,
            color = Color.Black
        )

        // User Email
        Text(
            text = userEmail,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Edit Profile Button
        Button(
            onClick = {
                // Handle edit profile button click
                navController.navigate("edit_profile")
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Edit Profile")
        }

        // Navigate to SMS Screen Button
        Button(
            onClick = {
                navController.navigate("sms") // Navigate to the SMSScreen
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(text = "Send SMS")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    // Provide a dummy NavHostController for preview
    ProfileScreen(navController = rememberNavController())
}
