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
import android.os.Build
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
import androidx.compose.runtime.LaunchedEffect
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
    val gnssAmplitude by viewModel.gnssAmplitude.collectAsState()

    // Collect GNSS location states
    val gnssLatitude by viewModel.gnssLatitude.collectAsState()
    val gnssLongitude by viewModel.gnssLongitude.collectAsState()


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
    var gnssLocationMarker by remember { mutableStateOf<MapMarker?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var lastKnownGnssLocation by remember { mutableStateOf<GeoCoordinates?>(null) }


    // Initialize FusedLocationProviderClient for continuous location updates
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationRequest = LocationRequest.create().apply {
        interval = 5000 // Update every 5 seconds
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }



    DisposableEffect(Unit) {
        activity?.let { act ->
            // Set landscape orientation
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(act.window, false)

            // Make window fullscreen
            act.window.apply {
                attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            }
        }

        // Create a context with English locale for MapView
        val locale = java.util.Locale("en", "US")
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        }
        val localizedContext = context.createConfigurationContext(config)

// Initialize MapView with localized context
        mapView = MapView(localizedContext).apply {
            onCreate(null)
            onResume()

            mapScene.loadScene(MapScheme.NORMAL_DAY) { error ->
                if (error == null) {
                    // Use a Western location as default (London, UK or New York, USA)
                    // London
                  //  val defaultCoordinates = GeoCoordinates(51.5074, -0.1278)
                    // Or use New York
                     val defaultCoordinates = GeoCoordinates(40.7128, -74.0060)

                    val zoomedOutDistance = MapMeasure(MapMeasure.Kind.DISTANCE, 9000000.0)
                    camera.lookAt(defaultCoordinates, zoomedOutDistance)

                    Log.i("JoyStickScreen", "Map loaded with default location: London")
                    isMapLoaded = true
                } else {
                    Log.e("JoyStickScreen", "Failed to load map: ${error.toString()}")
                }
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
            // Clean up map
            gnssLocationMarker?.let { mapView?.mapScene?.removeMapMarker(it) }
            mapView?.onPause()
            mapView?.onDestroy()
            mapView = null
        }
    }

    fun updateGnssMarker(location: GeoCoordinates) {
        // Create a blue dot for GNSS location
        val bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw outer white circle (border)
        val borderPaint = Paint().apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(20f, 20f, 20f, borderPaint)

        // Draw inner blue circle
        val paint = Paint().apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.BLUE
            isAntiAlias = true
        }
        canvas.drawCircle(20f, 20f, 16f, paint)

        // Draw center white dot
        val centerPaint = Paint().apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(20f, 20f, 4f, centerPaint)

        val mapImage = try {
            MapImageFactory.fromBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("JoyStickScreen", "Failed to create GNSS marker: ${e.message}", e)
            null
        }

        if (mapImage != null) {
            gnssLocationMarker?.let { mapView?.mapScene?.removeMapMarker(it) }
            gnssLocationMarker = MapMarker(location, mapImage).apply {
                mapView?.mapScene?.addMapMarker(this)
            }
        }
    }


    // Handle GNSS location updates
    LaunchedEffect(gnssLatitude, gnssLongitude) {
        if (mapView != null && isMapLoaded) {
            if (gnssLatitude != null && gnssLongitude != null) {
                // Valid GNSS location received
                val gnssLocation = GeoCoordinates(gnssLatitude!!, gnssLongitude!!)
                lastKnownGnssLocation = gnssLocation

                // Update camera to GNSS location with closer zoom
                val distance = MapMeasure(MapMeasure.Kind.DISTANCE, 1000.0) // 1km view
                mapView?.camera?.lookAt(gnssLocation, distance)

                // Update GNSS marker
                updateGnssMarker(gnssLocation)

                Log.i("JoyStickScreen", "GNSS location updated: $gnssLatitude, $gnssLongitude")
            } else {
                // No GNSS signal
                gnssLocationMarker?.let {
                    mapView?.mapScene?.removeMapMarker(it)
                    gnssLocationMarker = null
                }

                // Zoom out to show larger area
                val zoomOutLocation = lastKnownGnssLocation ?: GeoCoordinates(30.0444, 31.2357)
                val zoomedOutDistance = MapMeasure(MapMeasure.Kind.DISTANCE, 900000.0) // 50km view
                mapView?.camera?.lookAt(zoomOutLocation, zoomedOutDistance)

                Log.i("JoyStickScreen", "No GNSS signal - showing zoomed out view")
            }
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
                    BluetoothReaders(gnssAmplitude, "dB", Modifier.weight(1f)) // Add GNSS amplitude

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
