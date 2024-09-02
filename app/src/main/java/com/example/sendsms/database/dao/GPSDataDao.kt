package com.example.sendsms.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.sendsms.database.entity.GPSData
import kotlinx.coroutines.flow.Flow

@Dao
interface GPSDataDao {
    @Insert
    suspend fun insertGPSData(gpsData: GPSData)

    @Query("SELECT * FROM gps_data WHERE user_id = :userId ORDER BY timestamp DESC LIMIT 10")
    fun getRecentGPSDataForUser(userId: Int): Flow<List<GPSData>>

    @Query("SELECT * FROM gps_data WHERE user_id = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestGPSDataForUser(userId: Int): GPSData?

    @Query("DELETE FROM gps_data WHERE user_id = :userId")
    suspend fun removeAllGPSDataForUser(userId: Int)
}
