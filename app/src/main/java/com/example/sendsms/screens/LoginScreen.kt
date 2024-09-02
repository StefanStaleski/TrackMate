package com.example.sendsms.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.sendsms.components.AppButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sendsms.components.AppTextField
import com.example.sendsms.viewmodel.ApplicationViewModel
import com.example.sendsms.viewmodel.ApplicationViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavHostController,
    applicationViewModel: ApplicationViewModel = viewModel(
        factory = ApplicationViewModelFactory(LocalContext.current.applicationContext as Application)
    ),
    onLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginStatus by applicationViewModel.loginStatus.collectAsState()
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current

    fun validateInput(): Boolean {
        return when {
            username.length < 6 -> {
                errorMessage = "Username is required"
                false
            }
            password.isBlank() -> {
                errorMessage = "Password is required"
                false
            }
            else -> {
                errorMessage = ""
                true
            }
        }
    }

    LaunchedEffect(loginStatus) {
        if (loginStatus == "Invalid username or password") {
            errorMessage = loginStatus as String

            username = ""
            password = ""

            focusManager.clearFocus()

            kotlinx.coroutines.delay(2000)
            errorMessage = ""

            applicationViewModel.resetLoginStatus()
        } else if (loginStatus == "Login successful") {
            onLogin()
            navController.navigate("profile") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(5.5.dp))
        AppTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        AppTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (errorMessage.isNotEmpty()) {
            Text(
                text = "$errorMessage!",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        AppButton(
            text = "Login",
            onClick = {
                if (validateInput()) {
                    coroutineScope.launch {
                        applicationViewModel.login(username, password)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(
            onClick = {
                navController.navigate("register")
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFFFF6F61),
                disabledContentColor = Color.Gray
            )
        ) {
            Text("Not registered? Register here")
        }
    }
}
