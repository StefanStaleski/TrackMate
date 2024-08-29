package com.example.sendsms.viewmodel

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
    private val userPreferences: UserPreferences? = null
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _registrationStatus = MutableStateFlow<String?>(null)
    val registrationStatus: StateFlow<String?> = _registrationStatus

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

    fun getUserByUsername(username: String) {
        viewModelScope.launch {
            val user = userRepository.getUserByUsername(username)
                _user.value = user
        }
    }

    fun resetRegistrationStatus() {
        _registrationStatus.value = null
    }
}
