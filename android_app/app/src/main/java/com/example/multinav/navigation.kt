package com.example.multinav

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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.desgin.actions_delays_screen.ActionsAndDelaysScreen
import com.example.desgin.actions_delays_screen.ActionsAndDelaysViewModel
import com.example.multinav.actions_delays_screen.SetDelayScreen
import com.example.multinav.bluetooth.BluetoothDeviceScreen
import com.example.multinav.bluetooth.BluetoothViewModel
import com.example.multinav.chat.ChatScreen
import com.example.multinav.chat.ChatViewModel
import com.example.multinav.chat.ChatViewModelFactory
import com.example.multinav.login_screen.LoginScreen
import com.example.multinav.sign_up.SingUpScreen
import com.example.multinav.task_actions.TaskActionsScreen
import com.example.multinav.tasks_list.TaskSListScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val label: String? = null,
    val icon: Int? = null
) {
    object DeviceList : Screen("deviceList", label = "Devices", icon = R.drawable.ic_phone)
    object Chat : Screen("chat/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }
    object JoyStick : Screen("joystick/{deviceAddress}", label = "Joystick", icon = R.drawable.ic_joystick) {
        fun createRoute(deviceAddress: String) = "joystick/$deviceAddress"
    }
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ActionsAndDelays : Screen("actions_delays")
    object SetDelay : Screen("set_delay_screen")
    object TasksList : Screen("tasks_list")
    object TaskActions : Screen("task_actions/{taskTitle}/{taskId}") {
        fun createRoute(taskTitle: String, taskId: Int) = "task_actions/$taskTitle/$taskId"
    }
}

@Composable
fun Navigation(
    bluetoothViewModel: BluetoothViewModel,
    startDestination: String = Screen.DeviceList.route,
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
    val shouldShowNavBar =
        currentRoute != chatBaseRoute && currentRoute != Screen.Login.route && currentRoute != Screen.SignUp.route

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
                                        val connectedDevice =
                                            bluetoothViewModel.uiState.value.let { state ->
                                                state.pairedDevices.find { it.isConnected }
                                                    ?: state.scannedDevices.find { it.isConnected }
                                            }
                                        if (connectedDevice != null) {
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
                                            coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Connect to device first" ,
                                            duration = SnackbarDuration.Short
                                                )
                                           }
                                            navController.navigate(Screen.JoyStick.createRoute("PlaceHolder Address"))
                                            {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
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
                        isMobileDevice = bluetoothService.isMobileDevice,
                     //   navController = navController
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