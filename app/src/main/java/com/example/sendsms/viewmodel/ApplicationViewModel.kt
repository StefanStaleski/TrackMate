package com.example.sendsms.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sendsms.database.entity.AreaBoundaryData
import com.example.sendsms.database.entity.GPSData
import com.example.sendsms.database.entity.User
import com.example.sendsms.database.repository.AreaBoundaryDataRepository
import com.example.sendsms.database.repository.GPSDataRepository
import com.example.sendsms.database.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ApplicationViewModel(
    private val userRepository: UserRepository,
    private val gpsDataRepository: GPSDataRepository,
    private val areaBoundaryDataRepository: AreaBoundaryDataRepository,
    application: Application
) : ViewModel() {

    private val context: Context = application.applicationContext

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _registrationStatus = MutableStateFlow<String?>(null)
    val registrationStatus: StateFlow<String?> = _registrationStatus

    private val _loginStatus = MutableStateFlow<String?>(null)
    val loginStatus: StateFlow<String?> = _loginStatus

    private val _recentGPSData = MutableStateFlow<List<GPSData>>(emptyList())
    val recentGPSData: StateFlow<List<GPSData>> = _recentGPSData

    private val _areaBoundaries = MutableStateFlow<List<AreaBoundaryData>>(emptyList())
    val areaBoundaries: StateFlow<List<AreaBoundaryData>> = _areaBoundaries

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

                editor.putInt("userId", user.id)
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

    fun updateUser(username: String, newUsername: String?, newPassword: String?, newGpsLocatorNumber: String?) {
        viewModelScope.launch {
            val user = userRepository.getUserByUsername(username)
            if (user != null) {
                val updatedUser = user.copy(
                    username = newUsername ?: user.username,
                    password = newPassword ?: user.password,
                    gpsLocatorNumber = newGpsLocatorNumber ?: user.gpsLocatorNumber
                )
                userRepository.updateUser(updatedUser)

                _user.value = updatedUser

                Log.d("UserViewModel", "User successfully updated!")
            } else {
                Log.d("UserViewModel", "User not found for username: $username")
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

    fun insertGPSData(gpsData: GPSData) {
        viewModelScope.launch {
            gpsDataRepository.insertGPSData(gpsData)
            Log.d("ApplicationViewModel", "Successful GPS data save: $gpsData")
        }
    }

     fun getRecentGPSDataForUser(userId: Int) {
         viewModelScope.launch {
             gpsDataRepository.getRecentGPSDataForUser(userId).collect { data ->
                 _recentGPSData.value = data
                 println("ApplicationViewModel GPS Data: ${_recentGPSData.value}")
             }
         }
    }

    fun getLatestGPSDataForUser() {
        viewModelScope.launch {
            val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getInt("userId", -1)

            if (userId != -1) {
                val latestGPSData = gpsDataRepository.getLatestGPSDataForUser(userId)
                latestGPSData?.let {
                    val editor = sharedPreferences.edit()
                    editor.putInt("batteryPercentage", it.battery)
                    editor.putLong("lastBatteryCheck", it.timestamp)
                    editor.apply()
                }
            }
        }
    }

    fun removeAllGPSDataForUser() {
        viewModelScope.launch {
            val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getInt("userId", -1)

            if (userId != -1) {
                gpsDataRepository.removeAllGPSDataForUser(userId)
                _recentGPSData.value = emptyList()
                Log.d("ApplicationViewModel", "All GPS data deleted for userId: $userId")
            }
        }
    }

    fun insertAreaBoundaryData(areaBoundaryData: AreaBoundaryData) {
        viewModelScope.launch {
            areaBoundaryDataRepository.insert(areaBoundaryData)
            Log.d("ApplicationViewModel", "Successful Area Boundary Data save: $areaBoundaryData")

            val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getInt("userId", -1)
            if (userId != -1) {
                getBoundariesForUser(userId)
            }
        }
    }

    fun getBoundariesForUser(userId: Int) {
        viewModelScope.launch {
            areaBoundaryDataRepository.getBoundariesForUser(userId).let { boundaries ->
                _areaBoundaries.value = boundaries
                Log.d("ApplicationViewModel", "Area Boundaries: ${_areaBoundaries.value}")
            }
        }
    }

    fun removeBoundariesForUser(userId: Int) {
        viewModelScope.launch {
            areaBoundaryDataRepository.removeBoundariesForUser(userId)
            _areaBoundaries.value = emptyList()
            Log.d("ApplicationViewModel", "Removed all area boundaries for userId: $userId")
        }
    }

    fun removeBoundaryById(id: Int) {
        viewModelScope.launch {
            areaBoundaryDataRepository.deleteBoundaryById(id)
            
            // Refresh the boundaries list
            val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getInt("userId", -1)
            if (userId != -1) {
                getBoundariesForUser(userId)
            }
            
            Log.d("ApplicationViewModel", "Removed boundary with id: $id")
        }
    }
}
