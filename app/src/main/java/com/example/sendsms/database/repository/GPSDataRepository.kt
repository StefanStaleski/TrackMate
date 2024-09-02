package com.example.sendsms.database.repository

import com.example.sendsms.database.dao.GPSDataDao
import com.example.sendsms.database.entity.GPSData
import kotlinx.coroutines.flow.Flow

class GPSDataRepository(private val gpsDataDao: GPSDataDao) {

    suspend fun insertGPSData(gpsData: GPSData) {
        gpsDataDao.insertGPSData(gpsData)
    }

    fun getRecentGPSDataForUser(userId: Int): Flow<List<GPSData>> {
        return gpsDataDao.getRecentGPSDataForUser(userId)
    }

    suspend fun getLatestGPSDataForUser(userId: Int): GPSData? {
        return gpsDataDao.getLatestGPSDataForUser(userId)
    }

    suspend fun removeAllGPSDataForUser(userId: Int) {
        gpsDataDao.removeAllGPSDataForUser(userId)
    }
}
