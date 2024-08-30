package com.example.sendsms.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sendsms.database.dao.GPSDataDao
import com.example.sendsms.database.dao.UserDao
import com.example.sendsms.database.entity.GPSData
import com.example.sendsms.database.entity.User

@Database(entities = [User::class, GPSData::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun gpsDataDao(): GPSDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the gps_data table
                database.execSQL("""
            CREATE TABLE gps_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id INTEGER NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                battery INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES user(id) ON DELETE CASCADE
            )
        """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
