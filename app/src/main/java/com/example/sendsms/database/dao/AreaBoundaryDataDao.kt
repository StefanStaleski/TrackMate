package com.example.sendsms.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.sendsms.database.entity.AreaBoundaryData

@Dao
interface AreaBoundaryDataDao {

    @Insert
    suspend fun insert(areaBoundaryData: AreaBoundaryData)

    @Query("SELECT * FROM area_boundary_data WHERE user_id = :userId")
    suspend fun getBoundariesForUser(userId: Int): List<AreaBoundaryData>

    @Query("DELETE FROM area_boundary_data WHERE user_id = :userId")
    suspend fun removeBoundariesForUser(userId: Int)
}
