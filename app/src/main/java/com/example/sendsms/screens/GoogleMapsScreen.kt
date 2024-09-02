package com.example.sendsms.screens

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sendsms.viewmodel.ApplicationViewModel
import com.example.sendsms.components.BaseTemplate
import com.example.sendsms.components.RemoveMarkerItem
import com.example.sendsms.components.ToggleMarkersItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@Composable
fun GoogleMapsScreen(navController: NavHostController, applicationViewModel: ApplicationViewModel = viewModel(
    factory = ApplicationViewModelFactory(LocalContext.current.applicationContext as Application)
)) {
    val defaultLocation = LatLng(41.9981, 21.4254)

    var locationPermissionGranted by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var markersVisible by remember { mutableStateOf(true) }
    val googleMap by remember { mutableStateOf<com.google.android.gms.maps.GoogleMap?>(null) }
    val recentGPSData by applicationViewModel.recentGPSData.collectAsState()

    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    val userId = sharedPreferences.getInt("userId", -1)

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
        }
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
                    ) {
                        if (polylinePoints.size > 1) {
                            Polyline(
                                points = polylinePoints,
                                color = MaterialTheme.colorScheme.primary,
                                width = 8f
                            )
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