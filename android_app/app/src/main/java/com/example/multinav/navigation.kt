package com.example.multinav

import JoyStickScreen
import MainScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.multinav.bluetooth.BluetoothDeviceScreen
import com.example.multinav.bluetooth.BluetoothViewModel
import com.example.multinav.chat.ChatScreen
import com.example.multinav.chat.ChatViewModel
import com.example.multinav.chat.ChatViewModelFactory

sealed class Screen(
    val route: String,
    val label: String? = null,
    val icon: Int? = null // Change to Int for drawable resource ID
) {
   // object Main : Screen("main")
    object DeviceList : Screen("deviceList", label = "Device", icon = R.drawable.ic_device)
    object Chat : Screen("chat/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }
    object JoyStick : Screen("joystick", label = "Joystick", icon = R.drawable.ic_joystick) {
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
    val bluetoothService = bluetoothViewModel.bluetoothService

    // Get the current route to determine the selected tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/{deviceAddress}")

    // Extract the base route for Chat to compare correctly
    val chatBaseRoute = Screen.Chat.route.substringBefore("/{deviceAddress}") // This will be "chat"
    val shouldShowNavBar = currentRoute != chatBaseRoute

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = shouldShowNavBar,
                enter = slideInVertically(initialOffsetY = { it }), // Slide up when appearing
                exit = slideOutVertically(targetOffsetY = { it })   // Slide down when disappearing
            ) {
                NavigationBar(
                    modifier = Modifier.height(56.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    val tabItems = listOf(Screen.JoyStick, Screen.DeviceList)
                    tabItems.forEach { screen ->
                        if (screen.label != null && screen.icon != null) {
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(id = screen.icon),
                                        contentDescription = screen.label,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                },
                                label = {
                                    Text(
                                        screen.label,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
//            composable(Screen.Main.route) {
//                MainScreen(
//                    onNavigateToDevices = {
//                        navController.navigate(Screen.DeviceList.route)
//                    },
//                    onNavigateToJoystick = {
//                        navController.navigate(Screen.JoyStick.route)
//                    }
//                )
//            }
            composable(Screen.JoyStick.route) {
                JoyStickScreen(
                    bluetoothService = bluetoothService
                )
            }
            composable(Screen.DeviceList.route) {
                BluetoothDeviceScreen(
                    state = bluetoothViewModel.uiState.collectAsState().value,
                    bluetoothViewModel = bluetoothViewModel,
                    navController = navController
                )
            }
            composable(
                route = Screen.Chat.route
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
}