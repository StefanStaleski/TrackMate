package com.example.sendsms.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sendsms.database.entity.User
import com.example.sendsms.database.repository.UserRepository
import com.example.sendsms.utils.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences? = null,
    application: Application
) : ViewModel() {

    private val context: Context = application.applicationContext

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _registrationStatus = MutableStateFlow<String?>(null)
    val registrationStatus: StateFlow<String?> = _registrationStatus

    private val _loginStatus = MutableStateFlow<String?>(null)
    val loginStatus: StateFlow<String?> = _loginStatus

    suspend fun registerUser(username: String, password: String, gpsLocatorNumber: String) {
        val existingUser = userRepository.getUserByUsername(username)
        if (existingUser == null) {
            val newUser = User(username = username, password = password, gpsLocatorNumber = gpsLocatorNumber)
            viewModelScope.launch {
                userRepository.insertUser(newUser)
            }
            _registrationStatus.value =  "Registration successful"
        } else {
            _registrationStatus.value =  "User with that username already exists!"
        }

    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val user = userRepository.getUserByUsername(username)
            if (user != null && user.password == password) {
                _loginStatus.value = "Login successful"

                val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                editor.putString("username", username)
                editor.putString("password", password)
                editor.putString("gpsLocatorNumber", user.gpsLocatorNumber)
                editor.putBoolean("isLoggedIn", true)
                editor.apply() // or editor.commit() to make changes persistent

                // Log the updated preferences for debugging
                Log.d("UserViewModel", "Updated SharedPreferences: username=$username, gpsLocatorNumber=${user.gpsLocatorNumber}, isLoggedIn=${true}")
            } else {
                _loginStatus.value = "Invalid username or password"
            }
        }
    }

    fun getUserByUsername(username: String) {
        viewModelScope.launch {
            val user = userRepository.getUserByUsername(username)
                _user.value = user
        }
    }

    fun resetRegistrationStatus() {
        _registrationStatus.value = null
    }

    fun resetLoginStatus() {
        _loginStatus.value = null
    }
}
