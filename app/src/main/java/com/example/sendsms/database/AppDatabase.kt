package com.example.sendsms.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sendsms.database.dao.AreaBoundaryDataDao
import com.example.sendsms.database.dao.GPSDataDao
import com.example.sendsms.database.dao.UserDao
import com.example.sendsms.database.entity.AreaBoundaryData
import com.example.sendsms.database.entity.GPSData
import com.example.sendsms.database.entity.User

@Database(entities = [User::class, GPSData::class, AreaBoundaryData::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun gpsDataDao(): GPSDataDao
    abstract fun areaBoundaryDataDao(): AreaBoundaryDataDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the area_boundary_data table
                database.execSQL("""
                    CREATE TABLE area_boundary_data (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        user_id INTEGER NOT NULL,
                        point1Lat REAL NOT NULL,
                        point1Long REAL NOT NULL,
                        point2Lat REAL NOT NULL,
                        point2Long REAL NOT NULL,
                        point3Lat REAL NOT NULL,
                        point3Long REAL NOT NULL,
                        point4Lat REAL NOT NULL,
                        point4Long REAL NOT NULL,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
