@file:JvmName("JoyStickViewModelKt")



import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer

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

import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext


import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.desgin.constants.Modes
import com.example.joystick_Screen.JoyStickViewModel
import com.example.joystick_Screen.JoyStickViewModelFactory
import com.example.multinav.BluetoothService
import com.example.multinav.joystick_Screen.MyAnalogJoystick

import com.example.widgets.BluetoothReaders
import com.example.widgets.CircleIconButton
import com.example.widgets.CircleToggleButton

import com.example.widgets.FloatingButton
import com.example.widgets.RadioButtonMode
import kotlin.math.roundToInt


@Composable
fun JoyStickScreen(
    modifier: Modifier = Modifier,
    bluetoothService: BluetoothService,
    viewModel: JoyStickViewModel = viewModel(
        factory = JoyStickViewModelFactory(bluetoothService)
    )
) {

    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Scaffold()
    { innerPadding ->
        Column (
            modifier = Modifier.fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
           horizontalAlignment = Alignment.CenterHorizontally
        ){
            Row (modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .width(IntrinsicSize.Max),

                verticalAlignment = Alignment.CenterVertically,
            ){
                BluetoothReaders(bluetoothReader = "26", bluetoothReaderType = "°C",modifier = Modifier.weight(1f),)

                BluetoothReaders(bluetoothReader = "48", bluetoothReaderType = "%",modifier = Modifier.weight(1f),)

                BluetoothReaders(bluetoothReader = "1013", bluetoothReaderType = "hPa",modifier = Modifier.weight(1f),)

                BluetoothReaders(bluetoothReader = "Good", bluetoothReaderType = "",modifier = Modifier.weight(1f),)
            }

            Row (horizontalArrangement = Arrangement.Center,
                ){
                RadioButtonMode(
                    selectedModeState = viewModel.selectedMode,
                    modeName = Modes.MODE_ONE)

                RadioButtonMode(
                    selectedModeState = viewModel.selectedMode,
                    modeName = Modes.MODE_TWO)

                RadioButtonMode(
                    selectedModeState = viewModel.selectedMode,
                    modeName = Modes.MODE_THREE)
            }
            Box(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ){
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column (
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally){
                        CircleIconButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            contentDescription = "",
                            onCircleButtonClick = {},
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row{
                            CircleIconButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                contentDescription = "",
                                onCircleButtonClick = {},
                            )
                            Spacer(modifier = Modifier.width(32.dp))
                            CircleIconButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                contentDescription = "",
                                onCircleButtonClick = {},
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        CircleIconButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            contentDescription = "",
                            onCircleButtonClick = {},
                        )

                    }

                    Column (
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally

                    ){
                        Text(
                            text = "Current Angle: ${ viewModel.currentAngle.value.roundToInt()} degrees",
                            modifier = Modifier.padding(8.dp)
                        )
                        MyAnalogJoystick(
                            modifier = Modifier.size(200.dp),
                            onAngleChange = { angle ->
                                viewModel.currentAngle.value = angle
                            }
                        )

                    }


                    Column (modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally){
                        CircleIconButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            contentDescription = "",
                            onCircleButtonClick = {},
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            CircleIconButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                contentDescription = "",
                                onCircleButtonClick = {},
                            )
                            Spacer(modifier = Modifier.width(32.dp))
                            CircleIconButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                contentDescription = "",
                                onCircleButtonClick = {},
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CircleIconButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            contentDescription = "",
                            onCircleButtonClick = {},
                        )

                    }


                }

            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                CircleToggleButton(
                    buttonName = "A",
                    isToggled = viewModel.isToggleButtonA,
                    onButtonClick = {
                        isPressed ->
                        viewModel.onButtonAClick(isPressed)
                        Log.e("buttonA",isPressed.toString())
                    }
                )

                CircleToggleButton(
                    buttonName = "B",
                    isToggled = viewModel.isToggleButtonB,
                    onButtonClick = {
                        toggled ->
                    Log.e("ToggleButton B","${toggled}B")
                })

                CircleToggleButton(
                    buttonName = "C",
                    isToggled = viewModel.isToggleButtonC,
                    onButtonClick = {
                        toggled ->
                    Log.e("ToggleButton C","${toggled}C")
                })

                CircleToggleButton(buttonName = "D",
                    isToggled = viewModel.isToggleButtonD,
                    onButtonClick = {
                        toggled ->
                    Log.e("ToggleButton D","${toggled}D")
                })
            }
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()

            ){

                FloatingButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    onClick = {  }
                )
            }


        }
    }
}



//    @Preview(showSystemUi = true)
//    @Composable
//    private fun SecondScreenPre() {
//        JoyStickScreen(
//            bluetoothService = bluetoothService
//        )
//    }
//

