package com.example.multinav
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.multinav.chat.ChatScreen
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
        composable(Screen.DeviceList.route) {
            BluetoothDeviceScreen(
                state = bluetoothViewModel.state.collectAsState().value,
                onDeviceClick = { device ->
                    bluetoothViewModel.connectToDeviceAndNavigate(
                        device = device,
                        onNavigate = {
                            navController.navigate(Screen.Chat.createRoute(device.address))
                        }
                    )
                },
                bluetoothViewModel = bluetoothViewModel,
                navController = navController
            )
        }

        composable(
            route = Screen.Chat.route
        ) { backStackEntry ->
            val deviceAddress = backStackEntry.arguments?.getString("deviceAddress")
            deviceAddress?.let {
//                val chatViewModel: ChatViewModel = viewModel(
//                    factory = ChatViewModelFactory(deviceAddress, bluetoothService)
//                )
                ChatScreen(
                    viewModel = chatViewModel,
                    bluetoothService = bluetoothService,
                    onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}


