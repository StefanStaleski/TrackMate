package com.example.sendsms.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sendsms.ui.theme.GrayToBlackGradient
import androidx.navigation.NavHostController

@Composable
fun BaseTemplate(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomNavigation(navController)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GrayToBlackGradient)
                .padding(innerPadding)
        ) {
            content(Modifier.padding(16.dp))
        }
    }
}
