package com.example.multinav

import JoyStickScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multinav.bluetooth.BluetoothDeviceScreen
import com.example.multinav.bluetooth.BluetoothViewModel
import com.example.multinav.chat.ChatScreen
import com.example.multinav.chat.ChatViewModel
import com.example.multinav.chat.ChatViewModelFactory
import com.example.multinav.login_screen.LoginScreen
import com.example.multinav.sing_up.SingUpScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
<<<<<<< Updated upstream
=======
import kotlinx.coroutines.launch
>>>>>>> Stashed changes

sealed class Screen(val route: String) {
    object DeviceList : Screen("deviceList")
    object Chat : Screen("chat/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }
<<<<<<< Updated upstream
    object JoyStick : Screen("joystick/{deviceAddress}") { // Updated route to include deviceAddress
=======
    object JoyStick : Screen("joystick/{deviceAddress}", label = "Joystick", icon = R.drawable.ic_joystick) {
>>>>>>> Stashed changes
        fun createRoute(deviceAddress: String) = "joystick/$deviceAddress"
    }
    object Login : Screen("login")
    object SignUp : Screen("signup")
}

@Composable
fun Navigation(
    bluetoothViewModel: BluetoothViewModel,
<<<<<<< Updated upstream
    startDestination: String,
    auth: FirebaseAuth,
    database: FirebaseDatabase,
=======
    startDestination: String = Screen.DeviceList.route,
    auth: FirebaseAuth, // Added
    database: FirebaseDatabase // Added
>>>>>>> Stashed changes
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val bluetoothService = bluetoothViewModel.bluetoothService
<<<<<<< Updated upstream

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                auth = auth,
                navigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                navigateToMainScreen = { navController.navigate(Screen.DeviceList.route) }
            )
        }
        composable(Screen.SignUp.route) {
            SingUpScreen(
                auth = auth,
                database = database,
                navigateToLogin = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.JoyStick.route
        ) { backStackEntry ->
            val deviceAddress = backStackEntry.arguments?.getString("deviceAddress")
            deviceAddress?.let {
                JoyStickScreen(
                    bluetoothService = bluetoothService,
                    deviceAddress = deviceAddress // Pass deviceAddress if needed
                )
            }
        }
        composable(Screen.DeviceList.route) {
            BluetoothDeviceScreen(
                state = bluetoothViewModel.uiState.collectAsState().value,
                onDeviceSelected = { device -> // Updated parameter name to match expected usage
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
                val chatViewModel: ChatViewModel = viewModel(
                    factory = ChatViewModelFactory(deviceAddress, bluetoothService, isMobileDevice = true)
                )
                ChatScreen(
                    viewModel = chatViewModel,
                    bluetoothService = bluetoothService,
                    onNavigateBack = { navController.popBackStack() }
                )
            } ?: run {
                navController.popBackStack()
=======
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Get the current route to determine the selected tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/{deviceAddress}")

    // Extract the base route for Chat to compare correctly
    val chatBaseRoute = Screen.Chat.route.substringBefore("/{deviceAddress}")
    val shouldShowNavBar = currentRoute != chatBaseRoute && currentRoute != Screen.Login.route && currentRoute != Screen.SignUp.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = shouldShowNavBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
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
                                selected = currentRoute == screen.route.substringBefore("/{deviceAddress}"),
                                onClick = {
                                    if (screen.route == Screen.JoyStick.route) {
                                        // Find the connected device
                                        val connectedDevice = bluetoothViewModel.uiState.value.let { state ->
                                            state.pairedDevices.find { it.isConnected } ?: state.scannedDevices.find { it.isConnected }
                                        }
                                        if (connectedDevice != null) {
                                            navController.navigate(Screen.JoyStick.createRoute(connectedDevice.address)) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Connect to device first")
                                            }
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
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
            composable(Screen.Login.route) {
                LoginScreen(
                    auth = auth,
                    navigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    navigateToMainScreen = { navController.navigate(Screen.DeviceList.route) }
                )
            }
            composable(Screen.SignUp.route) {
                SingUpScreen(
                    auth = auth,
                    database = database,
                    navigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.JoyStick.route) { backStackEntry ->
                val deviceAddress = backStackEntry.arguments?.getString("deviceAddress")
                deviceAddress?.let {
                    JoyStickScreen(
                        bluetoothService = bluetoothService,
                        deviceAddress = it,
                        isMobileDevice = bluetoothService.isMobileDevice
                    )
                } ?: run {
                    Text("No device address provided", modifier = Modifier.padding(16.dp))
                }
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
                        isMobileDevice = true // Adjust based on your logic
                    )
                    val viewModel: ChatViewModel = viewModel(factory = factory)
                    ChatScreen(
                        deviceAddress = it,
                        bluetoothService = bluetoothService,
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = viewModel,
                    )
                }
>>>>>>> Stashed changes
            }
        }
    }
}