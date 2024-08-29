package com.example.sendsms.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import com.example.sendsms.components.BaseTemplate
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.google.android.gms.maps.model.LatLng

@Composable
fun GoogleMapsScreen(navController: NavHostController) {
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val context = LocalContext.current

    // Request location permission
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

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation ?: LatLng(37.7749, -122.4194), // Default to San Francisco coordinates if location not available
            10f
        )
    }

    // Update the camera position when the userLocation changes
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 17.5f)
        }
    }

    // Initialize UI settings
    val mapUiSettings = MapUiSettings(
        zoomControlsEnabled = true,
        myLocationButtonEnabled = locationPermissionGranted
    )

    BaseTemplate(navController = navController) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Google Maps Screen",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                cameraPositionState = cameraPositionState,
                uiSettings = mapUiSettings
            ) {
                // Example marker
                userLocation?.let {
                    val markerState = rememberMarkerState(position = it)
                    Marker(
                        state = markerState,
                        title = "Your Location"
                    )
                }
            }
        }
    }
}

fun fetchUserLocation(context: Context, onLocationFetched: (LatLng) -> Unit) {
    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Check if the ACCESS_FINE_LOCATION permission is granted
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
