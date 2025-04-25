package com.example.multinav

import ChatScreen
import JoyStickScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.multinav.ui.theme.MultiNavTheme
import android.Manifest
import android.os.Build
import android.util.Log
import com.example.multinav.chat.ChatViewModel
import com.example.multinav.chat.ChatViewModelFactory


class MainActivity : ComponentActivity() {
    private val bluetoothService by lazy { BluetoothService(this) }

    private val bluetoothViewModel by viewModels<BluetoothViewModel> {
        BluetoothViewModelFactory(bluetoothService)
    }


    private val chatViewModel by viewModels<ChatViewModel> {
        ChatViewModelFactory(null, bluetoothService)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Initialize Bluetooth - default to server mode
            bluetoothViewModel.startServer()
            Log.i("TAG", "perrmission checked: ")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        setContent {
            MultiNavTheme {
                Navigation(
                    bluetoothViewModel = bluetoothViewModel,
                    startDestination = Screen.DeviceList.route,
                            chatViewModel = chatViewModel // Pass the shared ChatViewModel

                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        } else {
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        // Optional: Add RECORD_AUDIO permission if voice features are needed
        permissions.add(Manifest.permission.RECORD_AUDIO)

        permissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.disconnect()
    }
}

//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            MultiNavTheme {
//                //    ChatScreen()
//                JoyStickScreen()
//
//            }
//        }
//    }
//}