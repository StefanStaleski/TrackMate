package com.example.sendsms.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.sendsms.database.AppDatabase
import com.example.sendsms.database.repository.AreaBoundaryDataRepository
import com.example.sendsms.database.repository.GPSDataRepository
import com.example.sendsms.utils.isPointInPolygon
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.runBlocking
import com.example.sendsms.services.NotificationHelper

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    private val sharedPreferences: SharedPreferences = appContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

    private val gpsDataRepository: GPSDataRepository by lazy {
        GPSDataRepository(AppDatabase.getDatabase(appContext).gpsDataDao())
    }

    private val areaBoundaryDataRepository: AreaBoundaryDataRepository by lazy {
        AreaBoundaryDataRepository(AppDatabase.getDatabase(appContext).areaBoundaryDataDao())
    }

    override fun doWork(): Result {
        val userId = sharedPreferences.getInt("userId", -1)
        if (userId == -1) {
            return Result.failure() // No userId found
        }

        return try {
            runBlocking {
                // Fetch the latest GPS data
                val latestGPSData = gpsDataRepository.getLatestGPSDataForUser(userId)
                    ?: return@runBlocking Result.failure() // No GPS data found

                val userLatLng = LatLng(latestGPSData.latitude, latestGPSData.longitude)

                val boundaries = areaBoundaryDataRepository.getBoundariesForUser(userId)
                Log.d("NotificationWorker", "Boundaries: $boundaries")

                if (boundaries.isEmpty()) {
                    val notificationHelper = NotificationHelper(applicationContext)
                    notificationHelper.sendNotification("Locator Alert", "You have no polygons set up")
                } else {
                    var isInsideAnyPolygon = false

                    for (boundary in boundaries) {
                        val polygon = listOf(
                            LatLng(boundary.point1Lat, boundary.point1Long),
                            LatLng(boundary.point2Lat, boundary.point2Long),
                            LatLng(boundary.point3Lat, boundary.point3Long),
                            LatLng(boundary.point4Lat, boundary.point4Long)
                        )
                        if (isPointInPolygon(userLatLng, polygon)) {
                            isInsideAnyPolygon = true
                            break
                        }
                    }

                    if (!isInsideAnyPolygon) {
                        val notificationHelper = NotificationHelper(applicationContext)
                        notificationHelper.sendNotification("Locator Alert", "Locator is outside the polygon zone")
                    }
                }

                Result.success()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
