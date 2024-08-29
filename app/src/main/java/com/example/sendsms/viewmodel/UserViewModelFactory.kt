package com.example.sendsms.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sendsms.database.AppDatabase
import com.example.sendsms.database.repository.UserRepository
import com.example.sendsms.utils.UserPreferences

class UserViewModelFactory(
    private val application: Application,
    private val usePreferences: Boolean = false,
) : ViewModelProvider.Factory {

    private val userRepository: UserRepository = UserRepository(
        AppDatabase.getDatabase(application).userDao()
    )

    private val userPreferences: UserPreferences? =
        if (usePreferences) UserPreferences(application) else null

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            return UserViewModel(userRepository, userPreferences, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
