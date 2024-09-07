package com.example.sendsms.database.repository

import com.example.sendsms.database.dao.AreaBoundaryDataDao
import com.example.sendsms.database.entity.AreaBoundaryData

class AreaBoundaryDataRepository(private val areaBoundaryDataDao: AreaBoundaryDataDao) {

    suspend fun insert(areaBoundaryData: AreaBoundaryData) {
        areaBoundaryDataDao.insert(areaBoundaryData)
    }

    suspend fun getBoundariesForUser(userId: Int): List<AreaBoundaryData> {
        return areaBoundaryDataDao.getBoundariesForUser(userId)
    }

    suspend fun removeBoundariesForUser(userId: Int) {
        areaBoundaryDataDao.removeBoundariesForUser(userId)
    }

    suspend fun deleteBoundaryById(id: Int) {
        areaBoundaryDataDao.deleteBoundaryById(id)
    }
}
