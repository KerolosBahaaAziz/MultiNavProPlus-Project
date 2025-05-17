package com.example.multinav

import ActionsAndDelaysScreen
import ActionsAndDelaysViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.joystick_Screen.JoyStickScreen
import androidx.navigation.navArgument

import com.example.multinav.actions_delays_screen.SetDelayScreen
import com.example.multinav.bluetooth.BluetoothDeviceScreen
import com.example.multinav.bluetooth.BluetoothViewModel
import com.example.multinav.chat.ChatScreen
import com.example.multinav.chat.ChatViewModel
import com.example.multinav.chat.ChatViewModelFactory
import com.example.multinav.login_screen.LoginScreen
import com.example.multinav.model.AudioRecorder
import com.example.multinav.sign_up.SignUpScreen
import com.example.multinav.splash_screen.SplashScreen
import com.example.multinav.task_actions.TaskActionsScreen
import com.example.multinav.tasks_list.TaskSListScreen
import com.example.multinav.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val label: String? = null,
    val icon: Int? = null
) {
    data object Splash : Screen("splash")
    data object DeviceList : Screen("deviceList", label = "Devices", icon = R.drawable.ic_phone)
    data object Chat : Screen("chat/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }

    data object JoyStick :
        Screen("joystick/{deviceAddress}", label = "Joystick", icon = R.drawable.ic_joystick) {
        fun createRoute(deviceAddress: String) = "joystick/$deviceAddress"
    }

    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object ActionsAndDelays : Screen("actions_delays")
    data object SetDelay : Screen("set_delay_screen")
    data object TasksList : Screen("tasks_list")
    data object TaskActions : Screen("task_actions/{taskTitle}/{taskId}") {
        fun createRoute(taskTitle: String, taskId: Int) = "task_actions/$taskTitle/$taskId"
    }
}

@Composable
fun Navigation(
    bluetoothViewModel: BluetoothViewModel,
    startDestination: String = Screen.Splash.route,
    auth: FirebaseAuth, // Added
    database: FirebaseDatabase // Added
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val bluetoothService = bluetoothViewModel.bluetoothService
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val actionsAndDelaysViewModel: ActionsAndDelaysViewModel = viewModel()
    // Get the current route to determine the selected tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/{deviceAddress}")

    // Extract the base route for Chat to compare correctly
    val chatBaseRoute = Screen.Chat.route.substringBefore("/{deviceAddress}")
    val shouldShowNavBar = currentRoute !=
            null && currentRoute != Screen.Splash.route &&
            currentRoute != chatBaseRoute && currentRoute != Screen.Login.route &&
            currentRoute != Screen.SignUp.route &&
            currentRoute != Screen.TasksList.route

    val gradientColors = listOf(
        Color(0xFF233992),
        Color(0xFFA030C7),
        Color(0xFF1C0090)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = shouldShowNavBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                // Wrapper Box for the navigation bar with gradient background
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                ) {
                    // Apply gradient background to the entire nav bar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = AppTheme.horizontalGradient)
                    )
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
                                        // First, check if we're already connected to a device
                                        val connectedDeviceAddress =
                                            bluetoothService.getConnectedDeviceAddress()

                                        if (connectedDeviceAddress != null) {
                                            // We have a connected device - use its address
                                            navController.navigate(
                                                Screen.JoyStick.createRoute(
                                                    connectedDeviceAddress
                                                )
                                            ) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        } else {
                                            // Find a device that's marked as connected in the UI state
                                            val connectedDevice =
                                                bluetoothViewModel.uiState.value.let { state ->
                                                    state.pairedDevices.find { it.isConnected }
                                                        ?: state.scannedDevices.find { it.isConnected }
                                                }

                                            if (connectedDevice != null) {
                                                // Navigate with the connected device from UI state
                                                navController.navigate(
                                                    Screen.JoyStick.createRoute(
                                                        connectedDevice.address
                                                    )
                                                ) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            } else {
                                                // No connected device found
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Connect to device first",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Regular navigation for other tabs
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
            composable(Screen.Splash.route) {
                SplashScreen()
                LaunchedEffect(Unit) {
                    delay(2000L) // Delay for 2 seconds
                    val nextRoute =
                        if (auth.currentUser != null && auth.currentUser!!.isEmailVerified) {

                            Screen.DeviceList.route
                        } else {
                            Screen.Login.route
                        }
                    navController.navigate(nextRoute) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    auth = auth,
                    navigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    navigateToMainScreen = { navController.navigate(Screen.DeviceList.route) }
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(
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
                        isMobileDevice = bluetoothService.isMobileDevice,
                        navController = navController
                    )
                } ?: run {
                    Text("No device address provided", modifier = Modifier.padding(16.dp))
                }
            }
            composable(Screen.DeviceList.route) {
                BluetoothDeviceScreen(
                    state = bluetoothViewModel.uiState.collectAsState().value,
                    bluetoothViewModel = bluetoothViewModel,
                    navController = navController,
                    auth = auth


                )
            }
            composable(
                route = Screen.Chat.route
            ) { backStackEntry ->
                val deviceAddress = backStackEntry.arguments?.getString("deviceAddress")
                deviceAddress?.let {

                    val audioRecorder = remember { AudioRecorder(context) }

                    val factory = ChatViewModelFactory(
                        deviceAddress = it,
                        bluetoothService = bluetoothService,
                        isMobileDevice = true,
                        audioRecorder = audioRecorder // Adjust based on your logic
                    )
                    val viewModel: ChatViewModel = viewModel(factory = factory)
                    ChatScreen(
                        deviceAddress = it,
                        bluetoothService = bluetoothService,
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = viewModel,
                    )
                }
            }
            composable(Screen.ActionsAndDelays.route) {

                ActionsAndDelaysScreen(
                    navController = navController,
                    viewModel = actionsAndDelaysViewModel
                )
            }
            // Added: SetDelayScreen route
            composable(Screen.SetDelay.route) {

                SetDelayScreen(
                    navController = navController,
                    viewModel = actionsAndDelaysViewModel
                )
            }

            composable(Screen.TasksList.route) {
                TaskSListScreen(
                    navController = navController
                )
            }
            composable(
                route = Screen.TaskActions.route,
                arguments = listOf(
                    navArgument("taskTitle") { type = NavType.StringType },
                    navArgument("taskId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                TaskActionsScreen(
                    navController = navController,
                    taskTitle = backStackEntry.arguments?.getString("taskTitle") ?: "",
                    taskId = backStackEntry.arguments?.getInt("taskId") ?: 0
                )
            }
        }
    }
}