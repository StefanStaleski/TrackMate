package com.example.sendsms.utils

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

/**
 * Calculates the minimum distance from a point to a polygon boundary as a percentage of the polygon's "radius"
 * Returns a value between 0 and 1, where 0 means the point is on the boundary and 1 means the point is at the center
 */
fun calculateDistanceToPolygonBoundaryPercentage(point: LatLng, polygon: List<LatLng>): Float {
    // If point is outside the polygon, return 0
    if (!isPointInPolygon(point, polygon)) {
        Log.d("PolygonUtils", "Point is outside polygon, returning 0")
        return 0f
    }
    
    // Find the minimum distance to any edge
    var minDistance = Float.MAX_VALUE
    
    for (i in polygon.indices) {
        val j = (i + 1) % polygon.size
        val distance = distanceToLine(point, polygon[i], polygon[j])
        minDistance = min(minDistance, distance.toFloat())
    }
    
    // Calculate the center of the polygon
    val center = calculatePolygonCenter(polygon)
    
    // Calculate the distance from point to center
    val distanceToCenter = haversineDistance(point, center)
    
    // Calculate the maximum possible distance (from center to furthest vertex)
    val maxDistance = polygon.map { haversineDistance(center, it) }.maxOrNull() ?: 1.0
    
    // Calculate the percentage (0 at boundary, 1 at center)
    // Formula: distance to boundary / maximum possible distance from boundary to center
    val percentage = (minDistance / maxDistance).toFloat()
    
    Log.d("PolygonUtils", "Distance calculation: minDistance=$minDistance, maxDistance=$maxDistance, distanceToCenter=$distanceToCenter, percentage=$percentage")
    
    return percentage
}

/**
 * Calculates the center of a polygon
 */
private fun calculatePolygonCenter(polygon: List<LatLng>): LatLng {
    val latSum = polygon.sumOf { it.latitude }
    val lngSum = polygon.sumOf { it.longitude }
    return LatLng(latSum / polygon.size, lngSum / polygon.size)
}

/**
 * Calculates the distance from a point to a line segment defined by two points
 * Returns the distance in meters
 */
private fun distanceToLine(point: LatLng, lineStart: LatLng, lineEnd: LatLng): Double {
    // Calculate the cross-track distance using the haversine formula
    val d13 = haversineDistance(lineStart, point)
    val d23 = haversineDistance(lineEnd, point)
    val d12 = haversineDistance(lineStart, lineEnd)
    
    // If the line segment is very short, just return the distance to either endpoint
    if (d12 < 1.0) {
        return min(d13, d23)
    }
    
    // Calculate the along-track distance
    val a = (d13 * d13 + d12 * d12 - d23 * d23) / (2 * d12)
    
    // If the closest point is outside the segment, return distance to the nearest endpoint
    if (a <= 0) return d13
    if (a >= d12) return d23
    
    // Calculate the cross-track distance using the Pythagorean theorem
    val crossTrackDistance = sqrt(d13 * d13 - a * a)
    
    return crossTrackDistance
}

/**
 * Calculates the haversine distance between two points in meters
 */
private fun haversineDistance(point1: LatLng, point2: LatLng): Double {
    val R = 6371000.0 // Earth radius in meters
    
    val lat1Rad = Math.toRadians(point1.latitude)
    val lat2Rad = Math.toRadians(point2.latitude)
    val latDiff = Math.toRadians(point2.latitude - point1.latitude)
    val lngDiff = Math.toRadians(point2.longitude - point1.longitude)
    
    val a = sin(latDiff / 2) * sin(latDiff / 2) +
            cos(lat1Rad) * cos(lat2Rad) *
            sin(lngDiff / 2) * sin(lngDiff / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    return R * c
} 