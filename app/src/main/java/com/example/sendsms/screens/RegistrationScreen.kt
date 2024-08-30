package com.example.sendsms.screens

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sendsms.viewmodel.ApplicationViewModel
import com.example.sendsms.viewmodel.ApplicationViewModelFactory
import com.example.sendsms.ui.theme.GrayToBlackGradient
import kotlinx.coroutines.launch

@Composable
fun RegistrationScreen(
    navController: NavHostController,
    applicationViewModel: ApplicationViewModel = viewModel(
        factory = ApplicationViewModelFactory(LocalContext.current.applicationContext as Application)
    ),
    onRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var locatorNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var isRegistrationSuccess by remember { mutableStateOf(false) }

    val registrationStatus by applicationViewModel.registrationStatus.collectAsState()
    val coroutineScope = rememberCoroutineScope()

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
                Log.d("RegistrationScreen", "LocatorNumber Length: ${locatorNumber.length}, locator number: $locatorNumber")
                errorMessage = "GPS Locator Number must be at least 9 digits long."
                false
            }
            else -> {
                errorMessage = ""
                true
            }
        }
    }

    LaunchedEffect(registrationStatus) {
        registrationStatus?.let { message ->
            isRegistrationSuccess = message == "Registration successful"

            val displayMessage = if (isRegistrationSuccess) {
                "$message! You will be redirected to the login page"
            } else {
                message
            }

            snackbarHostState.showSnackbar(
                message = displayMessage,
                duration = SnackbarDuration.Short,
            ).also {
                if (message == "Registration successful") {
                    onRegister()
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
                applicationViewModel.resetRegistrationStatus()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(GrayToBlackGradient)
                .padding(16.dp),
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
                onValueChange = { newLocatorNumber ->
                    locatorNumber = newLocatorNumber.filter { it.isDigit() } },
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
                        coroutineScope.launch {
                            applicationViewModel.registerUser(username, password, locatorNumber)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRegistrationSuccess
            ) {
                Text("Register")
            }
        }
    }
}
