package com.example.sendsms

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.sendsms.database.AppDatabase

class MyApplication : Application() {
    // Use lazy initialization to ensure the database is only created once
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }
}
