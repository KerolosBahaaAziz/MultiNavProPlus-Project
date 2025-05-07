@file:JvmName("JoyStickViewModelKt")
@file:Suppress("PreviewAnnotationInFunctionWithParameters")

package com.example.joystick_Screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.desgin.constants.Modes
import com.example.joystick_Screen.JoyStickViewModel
import com.example.joystick_Screen.JoyStickViewModelFactory
import com.example.multinav.BluetoothService
import com.example.multinav.R
import com.example.multinav.Screen
import com.example.multinav.joystick_Screen.MyAnalogJoystick
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.mapview.MapMeasure
import com.here.sdk.mapview.MapScheme
import com.here.sdk.mapview.MapView
import com.example.multinav.ui.theme.violetPurple

import com.example.widgets.BluetoothReaders
import com.example.widgets.CircleIconButton
import com.example.widgets.CircleToggleButton

import com.example.widgets.FloatingButton
import com.example.widgets.RadioButtonMode
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.here.sdk.gestures.TapListener
import com.here.sdk.mapview.MapImageFactory
import com.here.sdk.mapview.MapMarker
import kotlin.math.roundToInt

@Composable
fun CircleIconButton(
    icon: @Composable () -> Unit, // Changed to accept a composable lambda
    contentDescription: String,
    onCircleButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onCircleButtonClick,
        modifier = modifier
            .size(52.dp)
            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
        colors = IconButtonColors(
            containerColor = violetPurple,
            contentColor = Color.White,
            disabledContainerColor = violetPurple,
            disabledContentColor = violetPurple
        )

        ) {
        icon()
    }
}

@SuppressLint("MissingPermission")
@Preview(showSystemUi = true)
@Composable
fun JoyStickScreen(
    modifier: Modifier = Modifier,
    bluetoothService: BluetoothService,
    deviceAddress: String,
    isMobileDevice: Boolean,
    navController:NavController,
) {
    val viewModel: JoyStickViewModel = viewModel(
        factory = JoyStickViewModelFactory(bluetoothService, deviceAddress, isMobileDevice)
    )

    val context = LocalContext.current
    val activity = context as? Activity
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentLocation by remember { mutableStateOf<GeoCoordinates?>(null) }
    var locationMarker by remember { mutableStateOf<MapMarker?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var clickMarker by remember { mutableStateOf<MapMarker?>(null) }


    // Initialize FusedLocationProviderClient for continuous location updates
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationRequest = LocationRequest.create().apply {
        interval = 5000 // Update every 5 seconds
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }


    val locationCallback = remember {
        object : LocationCallback() {
            private var hasCameraUpdated = false // Flag to track if camera has been updated

            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                currentLocation = GeoCoordinates(location.latitude, location.longitude)
                Log.i(
                    "JoyStickScreen",
                    "Updated location: ${location.latitude}, ${location.longitude}"
                )

                // Update the camera only once
                if (!hasCameraUpdated) {
                    val distance = MapMeasure(MapMeasure.Kind.DISTANCE, 1000.0)
                    mapView?.camera?.lookAt(currentLocation!!, distance)
                    hasCameraUpdated = true // Set the flag to true after the first update
                    Log.i(
                        "JoyStickScreen",
                        "Camera updated to ${currentLocation!!.latitude}, ${currentLocation!!.longitude}"
                    )
                }

                // Add/update location marker if map is loaded
                if (isMapLoaded) {
                    addLocationMarker()
                }
            }

            fun addLocationMarker() {
                if (currentLocation == null) return

                // Create a programmatic red dot marker
                val bitmap = Bitmap.createBitmap(30, 30, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint().apply {
                    style = Paint.Style.FILL
                    color = android.graphics.Color.RED
                }
                canvas.drawCircle(14f, 14f, 14f, paint)

                val mapImage = try {
                    MapImageFactory.fromBitmap(bitmap)
                } catch (e: Exception) {
                    Log.e("JoyStickScreen", "Failed to create MapImage: ${e.message}", e)
                    null
                }
                if (mapImage != null) {
                    locationMarker?.let { mapView?.mapScene?.removeMapMarker(it) }
                    locationMarker = MapMarker(currentLocation!!, mapImage).apply {
                        mapView?.mapScene?.addMapMarker(this)
                        Log.i(
                            "JoyStickScreen",
                            "Location marker added at ${currentLocation!!.latitude}, ${currentLocation!!.longitude}"
                        )
                    }
                } else {
                    Log.w("JoyStickScreen", "MapImage is null, skipping location marker addition")
                }
            }
        }
    }
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Start location updates if permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {
            Log.e("JoyStickScreen", "Location permission not granted")
        }

        // Initialize MapView with tap listener
        mapView = MapView(context).apply {
            onCreate(null)
            onResume()
            mapScene.loadScene(MapScheme.NORMAL_DAY) { error ->
                if (error == null) {
                    val defaultCoordinates = GeoCoordinates(30.0444, 31.2357) // Cairo as fallback
                    val coordinates = currentLocation ?: defaultCoordinates
                    val distance = MapMeasure(MapMeasure.Kind.DISTANCE, 1000.0)
                    camera.lookAt(coordinates, distance)
                    Log.i(
                        "HERE Map",
                        "Map scene loaded at ${coordinates.latitude}, ${coordinates.longitude}"
                    )
                    isMapLoaded = true
                    // Add initial marker if location is available
                    if (currentLocation != null) {
                        locationCallback.addLocationMarker()
                    }
//                    // Add tap listener for map clicks
//                    gestures.tapListener = TapListener { point ->
//                        val geoCoordinates = viewToGeoCoordinates(point)
//                        if (geoCoordinates != null) {
//                            Log.i("JoyStickScreen", "Map clicked at ${geoCoordinates.latitude}, ${geoCoordinates.longitude}")
//                            // Create a red dot for click marker
//                            val clickBitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
//                            val clickCanvas = Canvas(clickBitmap)
//                            val clickPaint = Paint().apply {
//                                style = Paint.Style.FILL
//                                color =android.graphics.Color.DKGRAY
//                            }
//                            clickCanvas.drawCircle(10f, 10f, 10f, clickPaint)
//
//                            val clickMapImage = try {
//                                MapImageFactory.fromBitmap(clickBitmap)
//                            } catch (e: Exception) {
//                                Log.e("JoyStickScreen", "Failed to create click MapImage: ${e.message}", e)
//                                null
//                            }
//                            if (clickMapImage != null) {
//                                clickMarker?.let { mapScene.removeMapMarker(it) }
//                                clickMarker = MapMarker(geoCoordinates, clickMapImage).apply {
//                                    mapScene.addMapMarker(this)
//                                    Log.i("JoyStickScreen", "Click marker added at ${geoCoordinates.latitude}, ${geoCoordinates.longitude}")
//                                }
//                            }
//                        }
//                    }
                } else {
                    Log.e("HERE Map", "Failed to load map: ${error.toString()}")
                }
            }
        }

        onDispose {
            // Stop location updates
            fusedLocationClient.removeLocationUpdates(locationCallback)
            // Clean up markers
            locationMarker?.let { mapView?.mapScene?.removeMapMarker(it) }
            clickMarker?.let { mapView?.mapScene?.removeMapMarker(it) }
            mapView?.onPause()
            mapView?.onDestroy()
            mapView = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                // MapView
                AndroidView(
                    factory = { mapView!! },
                    update = { view -> view.onResume() },
                    modifier = Modifier.matchParentSize()
                )


                // Sensor Readings - Top Center
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BluetoothReaders("26", "°C", Modifier.weight(1f))
                    BluetoothReaders("48", "%", Modifier.weight(1f))
                    BluetoothReaders("1013", "hPa", Modifier.weight(1f))
                    BluetoothReaders("Good", "", Modifier.weight(1f))
                }

                // Joystick - Center
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(150.dp)
                        .background(Color.White.copy(alpha = 0.4f), CircleShape) // Add semi-transparent background
                ) {
                    Text(
                        text = "${viewModel.currentAngle.value.roundToInt()}°",
                        modifier = Modifier.align(Alignment.Center).fillMaxHeight(),
                        color = Color.Black
                    )
                    MyAnalogJoystick(
                        modifier = Modifier.matchParentSize(),
                        onAngleChange = { angle -> viewModel.currentAngle.value = angle }
                    )
                }

                // Arrows - Left Side
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircleIconButton(
                        icon = { Icon(Icons.Default.KeyboardArrowUp, "Up", Modifier.size(24.dp)) },
                        contentDescription = "Up",
                        onCircleButtonClick = { viewModel.sendDirectionCommand("UP") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        CircleIconButton(
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    "Left",
                                    Modifier.size(24.dp)
                                )
                            },
                            contentDescription = "Left",
                            onCircleButtonClick = { viewModel.sendDirectionCommand("LEFT") }
                        )
                        Spacer(Modifier.width(16.dp))
                        CircleIconButton(
                            icon = {
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    "Right",
                                    Modifier.size(24.dp)
                                )
                            },
                            contentDescription = "Right",
                            onCircleButtonClick = { viewModel.sendDirectionCommand("RIGHT") }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    CircleIconButton(
                        icon = {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                "Down",
                                Modifier.size(24.dp)
                            )
                        },
                        contentDescription = "Down",
                        onCircleButtonClick = { viewModel.sendDirectionCommand("DOWN") }
                    )
                }

                // PlayStation Buttons - Right Side
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircleIconButton(
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_triangle),
                                "Triangle",
                                Modifier.size(24.dp)
                            )
                        },
                        contentDescription = "Triangle",
                        onCircleButtonClick = { viewModel.sendActionCommand("TRIANGLE") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        CircleIconButton(
                            icon = {
                                Icon(
                                    painterResource(R.drawable.ic_square),
                                    "Square",
                                    Modifier.size(24.dp)
                                )
                            },
                            contentDescription = "Square",
                            onCircleButtonClick = { viewModel.sendActionCommand("SQUARE") }
                        )
                        Spacer(Modifier.width(16.dp))
                        CircleIconButton(
                            icon = {
                                Icon(
                                    painterResource(R.drawable.ic_circle),
                                    "Circle",
                                    Modifier.size(24.dp)
                                )
                            },
                            contentDescription = "Circle",
                            onCircleButtonClick = { viewModel.sendActionCommand("CIRCLE") }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    CircleIconButton(
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_x),
                                "Cross",
                                Modifier.size(24.dp)
                            )
                        },
                        contentDescription = "Cross",
                        onCircleButtonClick = { viewModel.sendActionCommand("CROSS") }
                    )

                   // Spacer(modifier = Modifier.height(70.dp))

                }

                // Radio Buttons - Below Joystick, Centered
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 60.dp), // Adjust to position above toggle buttons
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButtonMode(
                        selectedModeState = viewModel.selectedMode,
                        modeName = Modes.MODE_ONE
                    )
                    Spacer(Modifier.width(8.dp))
                    RadioButtonMode(
                        selectedModeState = viewModel.selectedMode,
                        modeName = Modes.MODE_TWO
                    )
                    Spacer(Modifier.width(8.dp))
                    RadioButtonMode(
                        selectedModeState = viewModel.selectedMode,
                        modeName = Modes.MODE_THREE
                    )
                }

                // Toggle Buttons - Bottom Center
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircleToggleButton(
                        buttonName = "A",
                        isToggled = viewModel.isToggleButtonA,
                        onButtonClick = { isPressed -> viewModel.onButtonAClick(isPressed) }
                    )
                    Spacer(Modifier.width(8.dp))
                    CircleToggleButton(
                        buttonName = "B",
                        isToggled = viewModel.isToggleButtonB,
                        onButtonClick = { toggled -> /* handle B */ }
                    )
                    Spacer(Modifier.width(8.dp))
                    CircleToggleButton(
                        buttonName = "C",
                        isToggled = viewModel.isToggleButtonC,
                        onButtonClick = { toggled -> /* handle C */ }
                    )
                    Spacer(Modifier.width(8.dp))
                    CircleToggleButton(
                        buttonName = "D",
                        isToggled = viewModel.isToggleButtonD,
                        onButtonClick = { toggled -> /* handle D */ }
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(50.dp)
                ) {
                    FloatingButton(
                        onClick = { navController.navigate(Screen.TasksList.route) },
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}


