package com.example.sendsms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sendsms.database.entity.User
import com.example.sendsms.database.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    fun registerUser(username: String, password: String, gpsLocatorNumber: String) {
        val newUser = User(username = username, password = password, gpsLocatorNumber = gpsLocatorNumber)
        viewModelScope.launch {
            userRepository.insertUser(newUser)
        }
    }

    fun getUserByUsername(username: String) {
        viewModelScope.launch {
            userRepository.getUserByUsername(username).collect { user ->
                _user.value = user
            }
        }
    }
}
