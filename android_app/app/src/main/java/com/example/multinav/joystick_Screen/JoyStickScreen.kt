@file:JvmName("JoyStickViewModelKt")
@file:Suppress("PreviewAnnotationInFunctionWithParameters")


import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.compose.foundation.background

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight

import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource


import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.desgin.constants.Modes
import com.example.joystick_Screen.JoyStickViewModel
import com.example.joystick_Screen.JoyStickViewModelFactory
import com.example.multinav.BluetoothService
import com.example.multinav.R
import com.example.multinav.joystick_Screen.MyAnalogJoystick

import com.example.widgets.BluetoothReaders
import com.example.widgets.CircleIconButton
import com.example.widgets.CircleToggleButton

import com.example.widgets.FloatingButton
import com.example.widgets.RadioButtonMode
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.mapview.MapMeasure
import com.here.sdk.mapview.MapScene
import com.here.sdk.mapview.MapScheme
import com.here.sdk.mapview.MapView
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
            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
    ) {
        icon()
    }
}



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
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    val mapView = remember { MapView(context) }

    // Map lifecycle management
    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        mapView.onCreate(null) // Initial creation

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mapView.onDestroy()
        }
    }

    // Handle screen orientation
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }



    AndroidView(
        factory = { mapView },
        update = { view ->
            try {
                // Try the state check if available in your SDK version
                val sceneState = view.mapScene::class.java.getDeclaredField("state").get(view.mapScene)
                if (sceneState.toString() != "LOADED") {
                    loadMapScene(view)
                }
            } catch (e: Exception) {
                // Fallback to boolean check
                if (true) {
                    loadMapScene(view)
                }
            }
        }
    )



    // ... rest of your UI code ...
}

fun loadMapScene(view: MapView) {
    view.mapScene.loadScene(MapScheme.NORMAL_DAY) { error ->
        if (error == null) {
            view.camera.lookAt(
                GeoCoordinates(52.5200, 13.4050),
                MapMeasure(MapMeasure.Kind.DISTANCE, 1000.0)
            )
        }
    }
}

//@Preview(showSystemUi = true)
//@Composable
//fun JoyStickScreen(
//    modifier: Modifier = Modifier,
//    bluetoothService: BluetoothService,
//    deviceAddress: String,
//    isMobileDevice: Boolean
//) {
//    val viewModel: JoyStickViewModel = viewModel(
//        factory = JoyStickViewModelFactory(bluetoothService, deviceAddress, isMobileDevice)
//    )
//
//    val context = LocalContext.current
//    val activity = context as? Activity
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val mapView = remember { MapView(context) }
//
////    DisposableEffect(Unit) {
////        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
////        onDispose {
////            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
////        }
////    }
//
//    // Manage MapView lifecycle with Compose lifecycle
//    DisposableEffect(Unit) {
//        // Set screen orientation
//     //   activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//
//        // Initialize MapView
//        mapView.onCreate(null)
//        Log.d("JoyStickScreen", "MapView onCreate called")
//
//        // Load map scene
//        mapView.onResume()
//        mapView.mapScene.loadScene(MapScheme.NORMAL_DAY) { error ->
//            if (error == null) {
//                val geoCoordinates = GeoCoordinates(52.5200, 13.4050)
//                val distance = MapMeasure(MapMeasure.Kind.DISTANCE, 1000.0)
//                mapView.camera.lookAt(geoCoordinates, distance)
//                Log.i("HERE Map", "Map scene loaded successfully")
//            } else {
//                Log.e("HERE Map", "Failed to load map: ${error.toString()}")
//            }
//        }
//
//        onDispose {
//            // Cleanup MapView
//            mapView.onPause()
//            mapView.onDestroy()
//            Log.d("JoyStickScreen", "MapView onDestroy called")
//
//            // Reset screen orientation
//            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
//        }
//    }
//    Scaffold { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(innerPadding)
//                .verticalScroll(rememberScrollState()),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .horizontalScroll(rememberScrollState())
//                    .wrapContentWidth(Alignment.CenterHorizontally)
//                    .width(IntrinsicSize.Max),
//                verticalAlignment = Alignment.CenterVertically,
//            ) {
//                BluetoothReaders(bluetoothReader = "26", bluetoothReaderType = "°C", modifier = Modifier.weight(1f))
//                BluetoothReaders(bluetoothReader = "48", bluetoothReaderType = "%", modifier = Modifier.weight(1f))
//                BluetoothReaders(bluetoothReader = "1013", bluetoothReaderType = "hPa", modifier = Modifier.weight(1f))
//                BluetoothReaders(bluetoothReader = "Good", bluetoothReaderType = "", modifier = Modifier.weight(1f))
//            }
//
//            Row(
//                horizontalArrangement = Arrangement.Center,
//            ) {
//                RadioButtonMode(
//                    selectedModeState = viewModel.selectedMode,
//                    modeName = Modes.MODE_ONE
//                )
//                RadioButtonMode(
//                    selectedModeState = viewModel.selectedMode,
//                    modeName = Modes.MODE_TWO
//                )
//                RadioButtonMode(
//                    selectedModeState = viewModel.selectedMode,
//                    modeName = Modes.MODE_THREE
//                )
//            }
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 16.dp),
//                contentAlignment = Alignment.Center,
//            ) {
//// Map at the top
//                AndroidView(
//                    factory = { context ->
//                        MapView(context).apply {
//                            onCreate(null) // Initialize the MapView
//                            onResume()     // Ensure it’s active
//                            mapScene.loadScene(MapScheme.NORMAL_DAY) { error ->
//                                if (error == null) {
//                                    val geoCoordinates = GeoCoordinates(52.5200, 13.4050) // Berlin coordinates
//                                    val distance = MapMeasure(MapMeasure.Kind.DISTANCE, 1000.0)
//                                    camera.lookAt(geoCoordinates, distance)
//                                    Log.i("HERE Map", "Map scene loaded successfully")
//                                } else {
//                                    Log.e("HERE Map", "Failed to load map: ${error.toString()}")
//                                }
//                            }
//                        }
//                    },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(300.dp)
//                        .background(Color.Red) // Debug: Red background if map tiles fail
//                )
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .horizontalScroll(rememberScrollState()),
//                    horizontalArrangement = Arrangement.SpaceEvenly,
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//                    // Left Column (directional arrows)
//                    Column(
//                        modifier = Modifier.padding(8.dp),
//                        verticalArrangement = Arrangement.Center,
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        CircleIconButton(
//                            icon = {
//                                Icon(
//                                    imageVector = Icons.Default.KeyboardArrowUp,
//                                    contentDescription = "Up",
//                                    modifier = Modifier.size(20.dp),
//                                    tint = Color.White
//                                )
//                            },
//                            contentDescription = "Up",
//                            onCircleButtonClick = {},
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Row {
//                            CircleIconButton(
//                                icon = {
//                                    Icon(
//                                        imageVector = Icons.Default.KeyboardArrowLeft,
//                                        contentDescription = "Left",
//                                        modifier = Modifier.size(20.dp),
//                                        tint = Color.White
//                                    )
//                                },
//                                contentDescription = "Left",
//                                onCircleButtonClick = {},
//                            )
//                            Spacer(modifier = Modifier.width(32.dp))
//                            CircleIconButton(
//                                icon = {
//                                    Icon(
//                                        imageVector = Icons.Default.KeyboardArrowRight,
//                                        contentDescription = "Right",
//                                        modifier = Modifier.size(20.dp),
//                                        tint = Color.White
//                                    )
//                                },
//                                contentDescription = "Right",
//                                onCircleButtonClick = {},
//                            )
//                        }
//                        Spacer(modifier = Modifier.height(8.dp))
//                        CircleIconButton(
//                            icon = {
//                                Icon(
//                                    imageVector = Icons.Default.KeyboardArrowDown,
//                                    contentDescription = "Down",
//                                    modifier = Modifier.size(20.dp),
//                                    tint = Color.White
//                                )
//                            },
//                            contentDescription = "Down",
//                            onCircleButtonClick = {},
//                        )
//                    }
//
//                    // Middle Column (joystick)
//                    Column(
//                        modifier = Modifier.padding(8.dp),
//                        verticalArrangement = Arrangement.Center,
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Text(
//                            text = "Current Angle: ${viewModel.currentAngle.value.roundToInt()} degrees",
//                            modifier = Modifier.padding(8.dp)
//                        )
//                        MyAnalogJoystick(
//                            modifier = Modifier.size(200.dp),
//                            onAngleChange = { angle ->
//                                viewModel.currentAngle.value = angle
//                            }
//                        )
//                    }
//
//                    // Right Column (PlayStation buttons)
//                    Column(
//                        modifier = Modifier.padding(8.dp),
//                        verticalArrangement = Arrangement.Center,
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        // Top: Triangle
//                        CircleIconButton(
//                            icon = {
//                                Icon(
//                                    painter = painterResource(R.drawable.ic_triangle),
//                                    contentDescription = "Triangle",
//                                    modifier = Modifier.size(20.dp),
//                                    tint = Color.White
//                                )
//                            },
//                            contentDescription = "Triangle",
//                            onCircleButtonClick = {},
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//
//                        // Middle: Square (left) and Circle (right)
//                        Row {
//                            CircleIconButton(
//                                icon = {
//                                    Icon(
//                                        painter = painterResource(R.drawable.ic_square),
//                                        contentDescription = "Square",
//                                        modifier = Modifier.size(20.dp),
//                                        tint = Color.White
//                                    )
//                                },
//                                contentDescription = "Square",
//                                onCircleButtonClick = {},
//                            )
//                            Spacer(modifier = Modifier.width(32.dp))
//                            CircleIconButton(
//                                icon = {
//                                    Icon(
//                                        painter = painterResource(R.drawable.ic_circle),
//                                        contentDescription = "Circle",
//                                        modifier = Modifier.size(20.dp),
//                                        tint = Color.White
//                                    )
//                                },
//                                contentDescription = "Circle",
//                                onCircleButtonClick = {},
//                            )
//                        }
//                        Spacer(modifier = Modifier.height(8.dp))
//
//                        // Bottom: Cross
//                        CircleIconButton(
//                            icon = {
//                                Icon(
//                                    painter = painterResource(R.drawable.ic_x),
//                                    contentDescription = "Cross",
//                                    modifier = Modifier.size(20.dp),
//                                    tint = Color.White
//                                )
//                            },
//                            contentDescription = "Cross",
//                            onCircleButtonClick = {},
//                        )
//                    }
//                }
//            }
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
//            ) {
//                CircleToggleButton(
//                    buttonName = "A",
//                    isToggled = viewModel.isToggleButtonA,
//                    onButtonClick = { isPressed ->
//                        viewModel.onButtonAClick(isPressed)
//                        Log.e("buttonA", isPressed.toString())
//                    }
//                )
//
//                CircleToggleButton(
//                    buttonName = "B",
//                    isToggled = viewModel.isToggleButtonB,
//                    onButtonClick = { toggled ->
//                        Log.e("ToggleButton B", "${toggled}B")
//                    }
//                )
//
//                CircleToggleButton(
//                    buttonName = "C",
//                    isToggled = viewModel.isToggleButtonC,
//                    onButtonClick = { toggled ->
//                        Log.e("ToggleButton C", "${toggled}C")
//                    }
//                )
//
//                CircleToggleButton(
//                    buttonName = "D",
//                    isToggled = viewModel.isToggleButtonD,
//                    onButtonClick = { toggled ->
//                        Log.e("ToggleButton D", "${toggled}D")
//                    }
//                )
//            }
//
//            Box(
//                modifier = Modifier
//                    .padding(16.dp)
//                    .fillMaxWidth()
//            ) {
//                FloatingButton(
//                    modifier = Modifier
//                        .align(Alignment.BottomEnd)
//                        .padding(16.dp),
//                    onClick = { }
//                )
//            }
//        }
//    }
//}