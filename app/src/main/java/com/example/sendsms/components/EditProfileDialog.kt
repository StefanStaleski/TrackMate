package com.example.sendsms.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    currentUserName: String,
    currentGpsLocator: String
) {
    if (showDialog) {
        var userName by remember { mutableStateOf(currentUserName) }
        var gpsLocator by remember { mutableStateOf(currentGpsLocator) }

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF333333), // Updated to a mid-tone gray background
            title = {
                Text(
                    "Edit Profile",
                    color = Color.White,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(16.dp), // Padding inside the dialog
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Space between fields
                ) {
                    // Username TextField
                    TextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Username") },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color(0xFF2C2C2C), // Slightly lighter gray background for the TextField
                            focusedIndicatorColor = Color(0xFF4CAF50), // Green color for the focused indicator
                            unfocusedIndicatorColor = Color(0xFFB0B0B0), // Lighter gray for unfocused indicator
                            cursorColor = Color.White,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color(0xFFB0B0B0),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )

                    // GPS Locator TextField
                    TextField(
                        value = gpsLocator,
                        onValueChange = { gpsLocator = it },
                        label = { Text("GPS Locator Number") },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color(0xFF2C2C2C),
                            focusedIndicatorColor = Color(0xFF4CAF50),
                            unfocusedIndicatorColor = Color(0xFFB0B0B0),
                            cursorColor = Color.White,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color(0xFFB0B0B0),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSave(userName, gpsLocator)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Green color for the Save button
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0B0B0)) // Lighter gray color for the Cancel button
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            shape = RoundedCornerShape(16.dp) // Rounded corners for the dialog
        )
    }
}
