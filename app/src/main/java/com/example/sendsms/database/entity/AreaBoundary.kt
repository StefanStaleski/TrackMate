package com.example.sendsms.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "area_boundary_data",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class AreaBoundaryData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: Int,
    val point1Lat: Double,
    val point1Long: Double,
    val point2Lat: Double,
    val point2Long: Double,
    val point3Lat: Double,
    val point3Long: Double,
    val point4Lat: Double,
    val point4Long: Double
)
