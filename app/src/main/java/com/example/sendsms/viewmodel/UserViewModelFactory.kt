package com.example.sendsms.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sendsms.database.AppDatabase
import com.example.sendsms.database.repository.UserRepository

class UserViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {

    private val userRepository: UserRepository = UserRepository(
        AppDatabase.getDatabase(application).userDao()
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            return UserViewModel(userRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
