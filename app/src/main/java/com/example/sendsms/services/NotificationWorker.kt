package com.example.sendsms.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.sendsms.database.AppDatabase
import com.example.sendsms.database.repository.AreaBoundaryDataRepository
import com.example.sendsms.database.repository.GPSDataRepository
import com.example.sendsms.utils.calculateDistanceToPolygonBoundaryPercentage
import com.example.sendsms.utils.isPointInPolygon
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.runBlocking
import com.example.sendsms.services.NotificationHelper

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    private val sharedPreferences: SharedPreferences = appContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    private val notificationHelper = NotificationHelper(appContext)

    private val gpsDataRepository: GPSDataRepository by lazy {
        GPSDataRepository(AppDatabase.getDatabase(appContext).gpsDataDao())
    }

    private val areaBoundaryDataRepository: AreaBoundaryDataRepository by lazy {
        AreaBoundaryDataRepository(AppDatabase.getDatabase(appContext).areaBoundaryDataDao())
    }
    
    companion object {
        private const val TAG = "NotificationWorker"
        private const val BOUNDARY_PROXIMITY_THRESHOLD = 0.15f // 15% from boundary
        private const val PROXIMITY_NOTIFICATION_KEY = "last_proximity_notification_time"
        private const val PROXIMITY_NOTIFICATION_COOLDOWN = 5 * 60 * 1000 // 5 minutes in milliseconds
    }

    override fun doWork(): Result {
        val userId = sharedPreferences.getInt("userId", -1)
        if (userId == -1) {
            Log.d(TAG, "No userId found, skipping notification check")
            return Result.failure() // No userId found
        }

        return try {
            runBlocking {
                // Fetch the latest GPS data
                val latestGPSData = gpsDataRepository.getLatestGPSDataForUser(userId)
                    ?: return@runBlocking Result.failure() // No GPS data found

                val userLatLng = LatLng(latestGPSData.latitude, latestGPSData.longitude)
                Log.d(TAG, "Checking location for user $userId at ${userLatLng.latitude}, ${userLatLng.longitude}")

                val boundaries = areaBoundaryDataRepository.getBoundariesForUser(userId)
                Log.d(TAG, "Found ${boundaries.size} boundaries for user")

                if (boundaries.isEmpty()) {
                    notificationHelper.sendNotification(
                        "Locator Alert", 
                        "You have no polygons set up",
                        NotificationHelper.CHANNEL_LOCATION
                    )
                } else {
                    var isInsideAnyPolygon = false
                    var isCloseToExit = false
                    
                    for (boundary in boundaries) {
                        val polygon = listOf(
                            LatLng(boundary.point1Lat, boundary.point1Long),
                            LatLng(boundary.point2Lat, boundary.point2Long),
                            LatLng(boundary.point3Lat, boundary.point3Long),
                            LatLng(boundary.point4Lat, boundary.point4Long)
                        )
                        
                        if (isPointInPolygon(userLatLng, polygon)) {
                            isInsideAnyPolygon = true
                            
                            // Check if close to boundary
                            val distancePercentage = calculateDistanceToPolygonBoundaryPercentage(userLatLng, polygon)
                            Log.d(TAG, "Distance percentage to boundary: $distancePercentage")
                            
                            if (distancePercentage <= BOUNDARY_PROXIMITY_THRESHOLD) {
                                isCloseToExit = true
                                Log.d(TAG, "User is close to exit: distance percentage = $distancePercentage, threshold = $BOUNDARY_PROXIMITY_THRESHOLD")
                                break
                            }
                        }
                    }

                    val currentTime = System.currentTimeMillis()
                    
                    if (!isInsideAnyPolygon) {
                        // Outside polygon notification
                        Log.d(TAG, "Locator is outside the polygon zone, sending notification")
                        notificationHelper.sendNotification(
                            "Locator Alert", 
                            "Locator is outside the polygon zone",
                            NotificationHelper.CHANNEL_LOCATION
                        )
                    } else if (isCloseToExit) {
                        // Check cooldown for proximity notifications
                        val lastNotificationTime = sharedPreferences.getLong(PROXIMITY_NOTIFICATION_KEY, 0)
                        
                        if (currentTime - lastNotificationTime > PROXIMITY_NOTIFICATION_COOLDOWN) {
                            // Close to exit notification
                            Log.d(TAG, "Locator is close to the exit zone, sending notification")
                            notificationHelper.sendNotification(
                                "Proximity Alert", 
                                "GPS Locator is close to the exit zone of the perimeter",
                                NotificationHelper.CHANNEL_LOCATION
                            )
                            
                            // Update last notification time
                            sharedPreferences.edit()
                                .putLong(PROXIMITY_NOTIFICATION_KEY, currentTime)
                                .apply()
                        } else {
                            Log.d(TAG, "Skipping proximity notification due to cooldown. Time since last notification: ${(currentTime - lastNotificationTime) / 1000} seconds")
                        }
                    } else {
                        Log.d(TAG, "Locator is inside the polygon zone and not close to exit")
                    }
                }

                testProximityDetection()

                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in NotificationWorker", e)
            Result.failure()
        }
    }

    private fun testProximityDetection() {
        try {
            // Create a simple square polygon
            val polygon = listOf(
                LatLng(0.0, 0.0),
                LatLng(0.0, 0.01), // ~1.1km
                LatLng(0.01, 0.01),
                LatLng(0.01, 0.0)
            )
            
            // Test points
            val testPoints = listOf(
                Pair(LatLng(0.005, 0.005), "Center"), // Center
                Pair(LatLng(0.001, 0.005), "Near left edge"), // Near left edge
                Pair(LatLng(0.005, 0.001), "Near bottom edge"), // Near bottom edge
                Pair(LatLng(0.0001, 0.0001), "Very close to corner") // Very close to corner
            )
            
            for ((point, description) in testPoints) {
                val isInside = isPointInPolygon(point, polygon)
                val distancePercentage = calculateDistanceToPolygonBoundaryPercentage(point, polygon)
                val isClose = distancePercentage <= BOUNDARY_PROXIMITY_THRESHOLD
                
                Log.d(TAG, "Test point ($description): inside=$isInside, distancePercentage=$distancePercentage, isClose=$isClose")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in proximity test", e)
        }
    }
}
