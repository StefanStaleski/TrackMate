package com.example.sendsms.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sendsms.viewmodel.UserViewModel
import com.example.sendsms.viewmodel.UserViewModelFactory

@Composable
fun RegistrationScreen(
    userViewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var locatorNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    fun validateInput(): Boolean {
        return when {
            username.length < 6 -> {
                errorMessage = "Username must be at least 6 characters long."
                false
            }
            password.isBlank() -> {
                errorMessage = "Password is required."
                false
            }
            locatorNumber.length < 9 -> {
                errorMessage = "GPS Locator Number must be at least 9 digits long."
                false
            }
            else -> {
                errorMessage = ""
                true
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = locatorNumber,
            onValueChange = { locatorNumber = it },
            label = { Text("GPS Locator Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(
            onClick = {
                if (validateInput()) {
                    userViewModel.registerUser(username, password, locatorNumber)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }
    }
}
