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
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.desgin.constants.Modes
import com.example.multinav.model.bluetooth_service.BluetoothService
import com.example.multinav.R
import com.example.multinav.Screen
import com.example.multinav.joystick_Screen.MyAnalogJoystick
import com.example.multinav.settings.SettingsViewModel
import com.example.multinav.settings.SettingsViewModelFactory
import com.example.widgets.BluetoothReaders
import com.example.widgets.CircleToggleButton
import com.example.widgets.FloatingButton
import com.example.widgets.RadioButtonMode
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.mapview.MapImageFactory
import com.here.sdk.mapview.MapMarker
import com.here.sdk.mapview.MapMeasure
import com.here.sdk.mapview.MapScheme
import com.here.sdk.mapview.MapView
import kotlin.math.roundToInt

val gradientColors = listOf(
    Color(0xFF233992),
    Color(0xFFA030C7),
    Color(0xFF1C0090)
)

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
            .background(
                brush = Brush.linearGradient(colors = gradientColors.map { it.copy(alpha = 0.5f) }),
                shape = CircleShape
            )

    ) {
        icon()
    }
}

@SuppressLint("MissingPermission", "NewApi")
@Preview(showSystemUi = true)
@Composable
fun JoyStickScreen(
    modifier: Modifier = Modifier,
    bluetoothService: BluetoothService,
    deviceAddress: String,
    isMobileDevice: Boolean,
    navController: NavController,
    auth: FirebaseAuth, // Add this
    database: FirebaseDatabase // Add this
) {
    val viewModel: JoyStickViewModel = viewModel(
        factory = JoyStickViewModelFactory(bluetoothService, deviceAddress, isMobileDevice)
    )

    // Collect sensor states
    val temperature by viewModel.temperature.collectAsState()
    val humidity by viewModel.humidity.collectAsState()
    val pressure by viewModel.pressure.collectAsState()
    val airQuality by viewModel.airQuality.collectAsState()
    val showPremiumDialog = remember { mutableStateOf(false) }

    // Get the SettingsViewModel to check premium status
    val settingsFactory = SettingsViewModelFactory(
        auth = auth,
        databaseReference = database.reference
    )
    val settingsViewModel: SettingsViewModel = viewModel(factory = settingsFactory)
    val settingsState by settingsViewModel.uiState.collectAsState()
    Log.d(
        "JoyStickScreen",
        "UI update - Temp: $temperature, Humidity: $humidity, Pressure: $pressure, AQ: $airQuality"
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
        activity?.let { act ->
            // Set landscape orientation
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(act.window, false)

            // Make window fullscreen, extend into cutout area, and hide status bar
            act.window.apply {
                attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or  // Hide status bar
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            }
        }

        onDispose {
            activity?.let { act ->
                // Reset orientation and window flags
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.setDecorFitsSystemWindows(act.window, true)
                act.window.clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            }
        }
    }
    DisposableEffect(Unit) {
        // activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

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
            //  activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        //   containerColor = Color.Transparent, // Make scaffold background transparent
        modifier = Modifier.fillMaxSize()

        //  .systemBarsPadding()
    ) { innerPadding ->
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
                    .height(340.dp)
            ) {
                // MapView
                AndroidView(
                    factory = { mapView!! },
                    update = { view -> view.onResume() },
                    modifier = Modifier
                        .matchParentSize()
                        .fillMaxSize()
                )


                // Sensor Readings - Top Center
                // Update the Sensor Readings section to use the collected states
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BluetoothReaders(temperature, "°C", Modifier.weight(1f))
                    BluetoothReaders(humidity, "%", Modifier.weight(1f))
                    BluetoothReaders(pressure, "hPa", Modifier.weight(1f))
                    BluetoothReaders(airQuality, "", Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))


                // Radio Buttons - Below Joystick, Centered
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp), // Adjust to position above toggle buttons
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,

                    ) {
                    RadioButtonMode(
                        selectedModeState = viewModel.selectedMode,
                        modeName = Modes.MODE_ONE,
                        radioButtonColor = gradientColors.get(1)

                    )
                    Spacer(Modifier.width(8.dp))
                    RadioButtonMode(
                        selectedModeState = viewModel.selectedMode,
                        modeName = Modes.MODE_TWO,
                        radioButtonColor = gradientColors[1]

                    )
                    Spacer(Modifier.width(8.dp))
                    RadioButtonMode(
                        selectedModeState = viewModel.selectedMode,
                        modeName = Modes.MODE_THREE,
                        radioButtonColor = gradientColors[1]

                    )
                }

                // Joystick - Center
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(150.dp)
                        .background(
                            Color.White.copy(alpha = 0.4f),
                            CircleShape
                        ) // Add semi-transparent background
                ) {
                    Text(
                        text = "${viewModel.currentAngle.value.roundToInt()}°",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight(),
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
                        .padding(25.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircleIconButton(
                        icon = {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                "Up", Modifier.size(24.dp), tint = Color.White
                            )
                        },
                        contentDescription = "Up",
                        onCircleButtonClick = { viewModel.sendDirectionCommand("UP") }
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        CircleIconButton(
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    "Left",
                                    Modifier.size(24.dp), tint = Color.White

                                )
                            },
                            contentDescription = "Left",
                            onCircleButtonClick = { viewModel.sendDirectionCommand("LEFT") }
                        )
                        Spacer(Modifier.width(42.dp))
                        CircleIconButton(
                            icon = {
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    "Right",
                                    Modifier.size(24.dp), tint = Color.White

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
                                Modifier.size(24.dp), tint = Color.White

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
                        .padding(25.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircleIconButton(
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_triangle),
                                "Triangle",
                                Modifier.size(24.dp),
                                tint = Color.White

                            )
                        },
                        contentDescription = "Triangle",
                        onCircleButtonClick = { viewModel.sendActionCommand("TRIANGLE") }
                    )
                    Spacer(Modifier.height(10.dp))

                    Row {
                        CircleIconButton(
                            icon = {
                                Icon(
                                    painterResource(R.drawable.ic_square),
                                    "Square",
                                    Modifier.size(24.dp), tint = Color.White

                                )
                            },
                            contentDescription = "Square",
                            onCircleButtonClick = { viewModel.sendActionCommand("SQUARE") }
                        )
                        Spacer(Modifier.width(42.dp))
                        CircleIconButton(
                            icon = {
                                Icon(
                                    painterResource(R.drawable.ic_circle),
                                    "Circle",
                                    Modifier.size(24.dp), tint = Color.White

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
                                Modifier.size(24.dp), tint = Color.White

                            )
                        },
                        contentDescription = "Cross",
                        onCircleButtonClick = { viewModel.sendActionCommand("CROSS") }
                    )

                    // Spacer(modifier = Modifier.height(70.dp))

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
                        onClick = {
                            if (settingsState.isPremium) {
                                navController.navigate(Screen.TasksList.route)
                            } else {
                                showPremiumDialog.value = true
                            }
                        },
                        modifier = Modifier.size(50.dp)
                    )
                }

                // Add the premium dialog at the end of your composable
                if (showPremiumDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showPremiumDialog.value = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = {
                            Text(
                                text = "Premium Feature",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        text = {
                            Text(
                                text = "This feature requires a premium subscription. Would you like to subscribe now?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showPremiumDialog.value = false
                                    navController.navigate(Screen.Settings.route)
                                }
                            ) {
                                Text("Subscribe")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showPremiumDialog.value = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
