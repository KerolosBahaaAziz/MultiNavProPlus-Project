package com.example.multinav
import JoyStickScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.multinav.bluetooth.BluetoothDeviceScreen
import com.example.multinav.bluetooth.BluetoothViewModel
import com.example.multinav.chat.ChatScreen
import com.example.multinav.chat.ChatViewModel
import com.example.multinav.login_screen.LoginScreen
import com.example.multinav.main_screen.MainScreen
import com.example.multinav.sing_up.SingUpScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object DeviceList : Screen("deviceList")
    object Chat : Screen("chat/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }
    object JoyStick : Screen("joystick"){
        fun createRoute(deviceAddress: String) = "chat/$deviceAddress"
    }
    object Login : Screen("login")
    object SignUp : Screen("signup")

}

@Composable
fun Navigation(
    bluetoothViewModel: BluetoothViewModel,
    startDestination: String,
    chatViewModel: ChatViewModel,
    auth: FirebaseAuth,
    database: FirebaseDatabase,

) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val bluetoothService = bluetoothViewModel.bluetoothService // Access the shared BluetoothService from BluetoothViewModel


    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(auth = auth,
                navigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                navigateToMainScreen = { navController.navigate(Screen.Main.route) })
        }
        composable(Screen.SignUp.route) {
            SingUpScreen(
                auth = auth,
                database = database,
                navigateToLogin = { navController.popBackStack() })  //Pass navController
        }
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

        composable(Screen.DeviceList.route) {
            BluetoothDeviceScreen(
                state = bluetoothViewModel.uiState.collectAsState().value,
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


