package com.example.sendsms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.sendsms.R
import com.example.sendsms.components.BottomNavigationButton

@Composable
fun BottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent) // Set background to transparent
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavigationButton(
            navController = navController,
            route = "profile",
            isSelected = currentRoute == "profile",
            iconResId = R.drawable.person_24px // Replace with actual drawable resource ID
        )
        BottomNavigationButton(
            navController = navController,
            route = "actions",
            isSelected = currentRoute == "actions",
            iconResId = R.drawable.apps_24px // Replace with actual drawable resource ID
        )
        BottomNavigationButton(
            navController = navController,
            route = "settings",
            isSelected = currentRoute == "settings",
            iconResId = R.drawable.settings_24px // Replace with actual drawable resource ID
        )
    }
}
