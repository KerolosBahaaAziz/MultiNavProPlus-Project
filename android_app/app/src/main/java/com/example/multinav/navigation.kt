package com.example.multinav
import ChatScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.multinav.chat.ChatViewModel
import com.example.multinav.chat.ChatViewModelFactory

sealed class Screen(val route: String) {
    object DeviceList : Screen("deviceList")
    object Chat : Screen("chat/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }
}

@Composable
fun Navigation(
    bluetoothViewModel: BluetoothViewModel,
    startDestination: String = Screen.DeviceList.route
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.DeviceList.route) {
            BluetoothDeviceScreen(
                state = bluetoothViewModel.state.collectAsState().value,
                onDeviceClick = { device ->
                    bluetoothViewModel.connectToDevice(
                        device = device,
                        onSuccess = {
                            navController.navigate(Screen.Chat.createRoute(device.address))
                        }
                    )
                },
                bluetoothViewModel = bluetoothViewModel
            )
        }

        composable(
            route = Screen.Chat.route
        ) { backStackEntry ->
            val deviceAddress = backStackEntry.arguments?.getString("deviceAddress")
            deviceAddress?.let {
                val chatViewModel: ChatViewModel = viewModel(
                    factory = ChatViewModelFactory(deviceAddress)
                )
                ChatScreen(viewModel = chatViewModel)
            }
        }
    }
}