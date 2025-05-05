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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.desgin.constants.Modes
import com.example.joystick_Screen.JoyStickViewModel
import com.example.joystick_Screen.JoyStickViewModelFactory
import com.example.multinav.BluetoothService
import com.example.multinav.R
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
            .size(48.dp)
            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
        colors = IconButtonColors(
            containerColor = violetPurple,
            contentColor = violetPurple,
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
    isMobileDevice: Boolean
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
                Log.i("JoyStickScreen", "Updated location: ${location.latitude}, ${location.longitude}")

                // Update the camera only once
                if (!hasCameraUpdated) {
                    val distance = MapMeasure(MapMeasure.Kind.DISTANCE, 1000.0)
                    mapView?.camera?.lookAt(currentLocation!!, distance)
                    hasCameraUpdated = true // Set the flag to true after the first update
                    Log.i("JoyStickScreen", "Camera updated to ${currentLocation!!.latitude}, ${currentLocation!!.longitude}")
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
                        Log.i("JoyStickScreen", "Location marker added at ${currentLocation!!.latitude}, ${currentLocation!!.longitude}")
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
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
                    Log.i("HERE Map", "Map scene loaded at ${coordinates.latitude}, ${coordinates.longitude}")
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
            // Map with overlay controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                // Map at the bottom of the Box
                AndroidView(
                    factory = { mapView!! },
                    update = { view -> view.onResume() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Red.copy(alpha = 0.5f))
                )

                // Left side - Directional arrows overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Up arrow
                    CircleIconButton(
                        icon = {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                "Up",
                                Modifier.size(20.dp),
                                Color.White
                            )
                        },
                        contentDescription = "Up",
                        onCircleButtonClick = { viewModel.sendDirectionCommand("UP") },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Left/Right arrows
                    Row {
                        CircleIconButton(
                            icon = {
                                Icon(
                                    Icons.Default.KeyboardArrowLeft,
                                    "Left",
                                    Modifier.size(20.dp),
                                    Color.White
                                )
                            },
                            contentDescription = "Left",
                            onCircleButtonClick = { viewModel.sendDirectionCommand("LEFT") },
                        )

                        Spacer(modifier = Modifier.width(32.dp))

                        CircleIconButton(
                            icon = {
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    "Right",
                                    Modifier.size(20.dp),
                                    Color.White
                                )
                            },
                            contentDescription = "Right",
                            onCircleButtonClick = { viewModel.sendDirectionCommand("RIGHT") },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Down arrow
                    CircleIconButton(
                        icon = {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                "Down",
                                Modifier.size(20.dp),
                                Color.White
                            )
                        },
                        contentDescription = "Down",
                        onCircleButtonClick = { viewModel.sendDirectionCommand("DOWN") },
                    )
                }

                // Center - Analog Joystick
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(200.dp)
                ) {
                    MyAnalogJoystick(
                        modifier = Modifier.matchParentSize(),
                        onAngleChange = { angle -> viewModel.currentAngle.value = angle }
                    )
                }

                // Right side - PlayStation buttons overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Triangle button
                    CircleIconButton(
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_triangle),
                                "Triangle",
                                Modifier.size(20.dp),
                                Color.White
                            )
                        },
                        contentDescription = "Triangle",
                        onCircleButtonClick = { viewModel.sendActionCommand("TRIANGLE") },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Square/Circle buttons
                    Row {
                        CircleIconButton(
                            icon = {
                                Icon(
                                    painterResource(R.drawable.ic_square),
                                    "Square",
                                    Modifier.size(20.dp),
                                    Color.White
                                )
                            },
                            contentDescription = "Square",
                            onCircleButtonClick = { viewModel.sendActionCommand("SQUARE") },
                        )

                        Spacer(modifier = Modifier.width(32.dp))

                        CircleIconButton(
                            icon = {
                                Icon(
                                    painterResource(R.drawable.ic_circle),
                                    "Circle",
                                    Modifier.size(20.dp),
                                    Color.White
                                )
                            },
                            contentDescription = "Circle",
                            onCircleButtonClick = { viewModel.sendActionCommand("CIRCLE") },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cross button
                    CircleIconButton(
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_x),
                                "Cross",
                                Modifier.size(20.dp),
                                Color.White
                            )
                        },
                        contentDescription = "Cross",
                        onCircleButtonClick = { viewModel.sendActionCommand("CROSS") },
                    )
                }
            }

            // Rest of the UI components below the map
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .width(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BluetoothReaders(
                    bluetoothReader = "26",
                    bluetoothReaderType = "°C",
                    modifier = Modifier.weight(1f)
                )
                BluetoothReaders(
                    bluetoothReader = "48",
                    bluetoothReaderType = "%",
                    modifier = Modifier.weight(1f)
                )
                BluetoothReaders(
                    bluetoothReader = "1013",
                    bluetoothReaderType = "hPa",
                    modifier = Modifier.weight(1f)
                )
                BluetoothReaders(
                    bluetoothReader = "Good",
                    bluetoothReaderType = "",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.Center,
            ) {
                RadioButtonMode(
                    selectedModeState = viewModel.selectedMode,
                    modeName = Modes.MODE_ONE
                )
                RadioButtonMode(
                    selectedModeState = viewModel.selectedMode,
                    modeName = Modes.MODE_TWO
                )
                RadioButtonMode(
                    selectedModeState = viewModel.selectedMode,
                    modeName = Modes.MODE_THREE
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    16.dp,
                    Alignment.CenterHorizontally
                )
            ) {
                CircleToggleButton(
                    buttonName = "A",
                    isToggled = viewModel.isToggleButtonA,
                    onButtonClick = { isPressed ->
                        viewModel.onButtonAClick(isPressed)
                        Log.e("buttonA", isPressed.toString())
                    }
                )

                CircleToggleButton(
                    buttonName = "B",
                    isToggled = viewModel.isToggleButtonB,
                    onButtonClick = { toggled -> Log.e("ToggleButton B", "${toggled}B") }
                )

                CircleToggleButton(
                    buttonName = "C",
                    isToggled = viewModel.isToggleButtonC,
                    onButtonClick = { toggled -> Log.e("ToggleButton C", "${toggled}C") }
                )

                CircleToggleButton(
                    buttonName = "D",
                    isToggled = viewModel.isToggleButtonD,
                    onButtonClick = { toggled -> Log.e("ToggleButton D", "${toggled}D") }
                )
            }

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                FloatingButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    onClick = { }
                )
            }
        }
    }
}