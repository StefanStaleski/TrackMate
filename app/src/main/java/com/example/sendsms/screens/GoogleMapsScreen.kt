package com.example.sendsms.screens

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sendsms.viewmodel.ApplicationViewModel
import com.example.sendsms.components.BaseTemplate
import com.example.sendsms.components.RemoveBoundariesItem
import com.example.sendsms.components.RemoveMarkerItem
import com.example.sendsms.components.ToggleMarkersItem
import com.example.sendsms.database.entity.AreaBoundaryData
import com.example.sendsms.database.entity.GPSData
import com.example.sendsms.viewmodel.ApplicationViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Circle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import com.example.sendsms.utils.isPointInPolygon

@Composable
fun GoogleMapsScreen(navController: NavHostController, applicationViewModel: ApplicationViewModel = viewModel(
    factory = ApplicationViewModelFactory(LocalContext.current.applicationContext as Application)
)) {
    val defaultLocation = LatLng(41.9981, 21.4254)
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    val userId = sharedPreferences.getInt("userId", -1)

    var locationPermissionGranted by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var markersVisible by remember { mutableStateOf(true) }
    var boundaryPoints by remember { mutableStateOf<List<List<LatLng>>>(emptyList()) }
    var boundaryPointsForDrawing by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var boundaryVisible by remember { mutableStateOf(false) }
    var showSaveCancelButtons by remember { mutableStateOf(false) }
    var selectedPolygon: List<LatLng>? by remember { mutableStateOf(null) }
    var selectedPolygonId by remember { mutableStateOf<Int?>(null) }



    val googleMap by remember { mutableStateOf<com.google.android.gms.maps.GoogleMap?>(null) }
    val recentGPSData by applicationViewModel.recentGPSData.collectAsState()
    val areaBoundaries by applicationViewModel.areaBoundaries.collectAsState()



    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        locationPermissionGranted = isGranted
        if (isGranted) {
            fetchUserLocation(context) { location ->
                userLocation = location
            }
        }
    }

    LaunchedEffect(Unit) {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(userId) {
        if (userId != -1) {
            applicationViewModel.getRecentGPSDataForUser(userId)
            applicationViewModel.getBoundariesForUser(userId)
        }
    }

    LaunchedEffect(areaBoundaries) {
        Log.d("AREA", "BOUNDARIES: $areaBoundaries")
        boundaryPoints = areaBoundaries.filter { it.userId == userId }.map { boundary ->
            listOf(
                LatLng(boundary.point1Lat, boundary.point1Long),
                LatLng(boundary.point2Lat, boundary.point2Long),
                LatLng(boundary.point3Lat, boundary.point3Long),
                LatLng(boundary.point4Lat, boundary.point4Long)
            )
        }
        boundaryVisible = boundaryPoints.isNotEmpty()
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation ?: defaultLocation,
            10f
        )
    }

    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 17.5f)
        }
    }

    val mapUiSettings = MapUiSettings(
        zoomControlsEnabled = true,
        myLocationButtonEnabled = locationPermissionGranted,
    )

    val polylinePoints = remember(recentGPSData) {
        recentGPSData.map { LatLng(it.latitude, it.longitude) }
    }

    BaseTemplate(navController = navController) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(550.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = mapUiSettings,
                        onMapLoaded = {
                            googleMap?.mapType = com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
                        },
                        onMapClick = { latLng ->
                            val isInsidePolygon = boundaryPoints.any { polygon ->
                                isPointInPolygon(latLng, polygon)
                            }

                            if (isInsidePolygon) {
                                selectedPolygonId = areaBoundaries.firstOrNull { boundary ->
                                    isPointInPolygon(latLng, listOf(
                                        LatLng(boundary.point1Lat, boundary.point1Long),
                                        LatLng(boundary.point2Lat, boundary.point2Long),
                                        LatLng(boundary.point3Lat, boundary.point3Long),
                                        LatLng(boundary.point4Lat, boundary.point4Long)
                                    ))
                                }?.id
                                showSaveCancelButtons = false
                                boundaryPointsForDrawing = emptyList() // Clear any ongoing drawing
                            } else if (selectedPolygonId == null && boundaryPointsForDrawing.size < 4) {
                                // Continue drawing a new polygon
                                boundaryPointsForDrawing = boundaryPointsForDrawing + latLng
                                boundaryVisible = true
                                showSaveCancelButtons = boundaryPointsForDrawing.size > 3
                            }
                        }
                    ) {
                        boundaryPoints.forEach { points ->
                            Polygon(
                                points = points + if (points.size == 4) listOf(points.first()) else emptyList(),
                                fillColor = Color(0x5500FF00), // Transparent green fill
                                strokeColor = Color.Black, // Black stroke
                                strokeWidth = 2f,
                                onClick = {
                                    // Select the clicked polygon and prevent new boundary points from being drawn
                                    selectedPolygonId = areaBoundaries.firstOrNull { boundary ->
                                        isPointInPolygon(points.first(), listOf(
                                            LatLng(boundary.point1Lat, boundary.point1Long),
                                            LatLng(boundary.point2Lat, boundary.point2Long),
                                            LatLng(boundary.point3Lat, boundary.point3Long),
                                            LatLng(boundary.point4Lat, boundary.point4Long)
                                        ))
                                    }?.id
                                    showSaveCancelButtons = false
                                    boundaryPointsForDrawing = emptyList() // Clear any ongoing drawing
                                }
                            )
                        }

                        if (polylinePoints.size > 1) {
                            Polyline(
                                points = polylinePoints,
                                color = MaterialTheme.colorScheme.primary,
                                width = 8f
                            )
                        }

                        if (boundaryVisible && boundaryPointsForDrawing.size >= 3) {
                            Polygon(
                                points = boundaryPointsForDrawing + if (boundaryPointsForDrawing.size == 4) listOf(boundaryPointsForDrawing.first()) else emptyList(), // Close the polygon if 4 points
                                fillColor = Color(0x5500FF00), // Transparent green fill
                                strokeColor = Color.Black, // Black stroke
                                strokeWidth = 2f
                            )
                        }

                        if (boundaryPointsForDrawing.size >= 2) {
                            Polyline(
                                points = boundaryPointsForDrawing + if (boundaryPointsForDrawing.size == 4) listOf(boundaryPointsForDrawing.first()) else emptyList(), // Connect back to the first point if 4 points
                                color = Color.Black, // Black lines
                                width = 4f
                            )
                        }

                        if (boundaryVisible) {
                            boundaryPointsForDrawing.forEach { position ->
                                Circle(
                                    center = position,
                                    radius = 3.5,
                                    fillColor = Color.White,
                                    strokeColor = Color.Black,
                                    strokeWidth = 2f
                                )
                            }
                        }

                        if (markersVisible) {
                            recentGPSData.reversed().forEachIndexed { index, gpsData ->
                                val position = LatLng(gpsData.latitude, gpsData.longitude)
                                val markerState = rememberMarkerState(position = position)

                                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                val formattedDate = dateFormat.format(Date(gpsData.timestamp))

                                Marker(
                                    state = markerState,
                                    title = "Location #${index + 1} on $formattedDate",
                                    snippet = "Battery: ${gpsData.battery}%"
                                )

                                println("Placing marker at: $position")
                            }
                        }
                    }
                    if (showSaveCancelButtons || selectedPolygonId != null) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (showSaveCancelButtons) {
                                Button(onClick = {
                                    if (boundaryPointsForDrawing.isNotEmpty() && userId != -1) {
                                        val areaBoundaryData = AreaBoundaryData(
                                            userId = userId,
                                            point1Lat = boundaryPointsForDrawing[0].latitude,
                                            point1Long = boundaryPointsForDrawing[0].longitude,
                                            point2Lat = boundaryPointsForDrawing[1].latitude,
                                            point2Long = boundaryPointsForDrawing[1].longitude,
                                            point3Lat = boundaryPointsForDrawing[2].latitude,
                                            point3Long = boundaryPointsForDrawing[2].longitude,
                                            point4Lat = boundaryPointsForDrawing[3].latitude,
                                            point4Long = boundaryPointsForDrawing[3].longitude
                                        )
                                        applicationViewModel.insertAreaBoundaryData(areaBoundaryData)

                                        boundaryPoints = boundaryPoints + listOf(boundaryPointsForDrawing)

                                        showSaveCancelButtons = false
                                        boundaryPointsForDrawing = emptyList()
                                        boundaryVisible = false
                                        applicationViewModel.getBoundariesForUser(userId)
                                    }
                                }) {
                                    Text("Save")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(onClick = {
                                    boundaryPointsForDrawing = emptyList() // Clear boundary points
                                    boundaryVisible = false
                                    showSaveCancelButtons = false // Hide buttons
                                }) {
                                    Text("Cancel")
                                }
                            }

                            if (selectedPolygonId != null) {
                                Button(onClick = {
                                    selectedPolygonId?.let { id ->
                                        applicationViewModel.removeBoundaryById(id)
                                    }
                                    boundaryPoints = boundaryPoints.filter { it != selectedPolygon }
                                    selectedPolygonId = null
                                }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }

                    // Cancel Button in the top right corner
                    if (selectedPolygonId != null) {
                        Button(
                            onClick = {
                                selectedPolygonId = null // Just deselect the polygon
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Text("Cancel")
                        }
                    }

                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    userLocation?.let { location ->
                        val batteryPercentage = Random.nextInt(1, 101)

                        if (userId != -1) {
                            val gpsData = GPSData(
                                userId = userId,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                battery = batteryPercentage,
                                timestamp = System.currentTimeMillis()
                            )

                            applicationViewModel.insertGPSData(gpsData)
                        }
                    }
                }) {
                    Text("Save Location Data")
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ToggleMarkersItem(
                    isMarkersVisible = markersVisible,
                    onClick = { markersVisible = !markersVisible },
                    modifier = Modifier
                        .padding(end = 8.dp)
                )

                RemoveBoundariesItem(
                    onClick = {
                        if (userId != -1) {
                            applicationViewModel.removeBoundariesForUser(userId)
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )

                RemoveMarkerItem(
                    onClick = {
                        applicationViewModel.removeAllGPSDataForUser()
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                )
            }
        }
    }
}

fun fetchUserLocation(context: Context, onLocationFetched: (LatLng) -> Unit) {
    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        return
    }

    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            location?.let {
                onLocationFetched(LatLng(it.latitude, it.longitude))
            }
        }
}