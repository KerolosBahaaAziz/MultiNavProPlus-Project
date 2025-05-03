package com.example.multinav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.multinav.ui.theme.MultiNavTheme
import android.Manifest
import android.os.Build
import android.util.Log
import com.example.multinav.bluetooth.BluetoothViewModel
import com.example.multinav.bluetooth.BluetoothViewModelFactory
import com.example.multinav.chat.ChatViewModel
import com.example.multinav.chat.ChatViewModelFactory
import com.example.multinav.login_screen.LoginScreen
import com.example.multinav.sing_up.SingUpScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.here.sdk.core.engine.SDKNativeEngine
import com.here.sdk.core.engine.SDKOptions
import com.here.sdk.core.errors.InstantiationErrorException

class MainActivity : ComponentActivity() {
    private val bluetoothService by lazy { BluetoothService(this) }

    private val bluetoothViewModel by viewModels<BluetoothViewModel> {
        BluetoothViewModelFactory(bluetoothService)
    }

    private val audioRecorder by lazy { AudioRecorder(this) }

    // Initialize ChatViewModel with AudioRecorder
    private val chatViewModel by viewModels<ChatViewModel> {
        ChatViewModelFactory(
            deviceAddress = null, // Will be set when navigating to ChatScreen
            bluetoothService = bluetoothService,
            isMobileDevice = false,
            audioRecorder = audioRecorder
        )
    }
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            bluetoothViewModel.startServer()
            Log.i("TAG", "permissions checked")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        val auth = FirebaseAuth.getInstance()
        val database : FirebaseDatabase = FirebaseDatabase.getInstance()

        // Initialize HERE SDK with SDKOptions using resources
        val apiKey = getString(R.string.here_api_key)
        val sdkOptions = SDKOptions(apiKey, "").apply {  // Using API key as accessKeyId, empty secret
            cachePath = "${filesDir}/here_sdk_cache"
        }
        try {
            SDKNativeEngine.makeSharedInstance(this, sdkOptions)
        } catch (e: InstantiationErrorException) {
            Log.e("MainActivity", "Failed to initialize HERE SDK: ${e.message}")
            throw RuntimeException("HERE SDK initialization failed", e)
        }

        setContent {
            MultiNavTheme {
          //     SingUpScreen(auth = auth)
                Navigation(
                    bluetoothViewModel = bluetoothViewModel,
                    database = database,
                    auth = auth,
                    startDestination = "login")
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
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        }

        permissions.add(Manifest.permission.RECORD_AUDIO)

        permissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.disconnect()
        // Clean up HERE SDK
        SDKNativeEngine.getSharedInstance()?.dispose()
    }

}