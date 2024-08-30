package com.example.sendsms.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sendsms.database.AppDatabase
import com.example.sendsms.database.repository.GPSDataRepository
import com.example.sendsms.database.repository.UserRepository

class ApplicationViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {

    private val userRepository: UserRepository = UserRepository(
        AppDatabase.getDatabase(application).userDao()
    )

    private val gpsDataRepository: GPSDataRepository = GPSDataRepository(
        AppDatabase.getDatabase(application).gpsDataDao()
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ApplicationViewModel::class.java)) {
            return ApplicationViewModel(userRepository, gpsDataRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
