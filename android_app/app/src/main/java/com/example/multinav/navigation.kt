package com.example.multinav
import JoyStickScreen
import MainScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.multinav.bluetooth.BluetoothDeviceScreen
import com.example.multinav.bluetooth.BluetoothViewModel
import com.example.multinav.chat.ChatScreen
import com.example.multinav.chat.ChatViewModel
import com.example.multinav.chat.ChatViewModelFactory

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object DeviceList : Screen("deviceList")
    object Chat : Screen("chat/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }
    object JoyStick : Screen("joystick"){
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }

}

@Composable
fun Navigation(
    bluetoothViewModel: BluetoothViewModel,
    startDestination: String = Screen.DeviceList.route,
    chatViewModel: ChatViewModel

) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val bluetoothService = bluetoothViewModel.bluetoothService // Access the shared BluetoothService from BluetoothViewModel


    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToDevices = {
                    navController.navigate(Screen.DeviceList.route)
                },
                onNavigateToJoystick = {
                    navController.navigate(Screen.JoyStick.route)
                }
            )
        }
        composable(Screen.JoyStick.route) {
            JoyStickScreen(
                bluetoothService = bluetoothService
            )
        }

//        composable(Screen.DeviceList.route) {
//            BluetoothDeviceScreen(
//                state = bluetoothViewModel.uiState.collectAsState().value,
//                onDeviceClick = { device ->
//                    bluetoothViewModel.connectToDeviceAndNavigate(
//                        device = device,
//                        onNavigate = {
//                            navController.navigate(Screen.Chat.createRoute(device.address))
//                        }
//                    )
//                },
//                bluetoothViewModel = bluetoothViewModel,
//                navController = navController
//            )
//        }


        composable(Screen.DeviceList.route) {
            BluetoothDeviceScreen(
                state = bluetoothViewModel.uiState.collectAsState().value,
                bluetoothViewModel = bluetoothViewModel,
                navController = navController
            )
        }

        composable(
            route = Screen.Chat.route // e.g., "chat/{deviceAddress}"
        ) { backStackEntry ->
            val deviceAddress = backStackEntry.arguments?.getString("deviceAddress")
            deviceAddress?.let {
                val factory = ChatViewModelFactory(
                    deviceAddress = it,
                    bluetoothService = bluetoothService,
                    isMobileDevice = false
                )
                val viewModel: ChatViewModel = viewModel(factory = factory)
                ChatScreen(
                    deviceAddress = it,
                    bluetoothService = bluetoothService,
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }
        }
    }
}


