@file:Suppress("DEPRECATION")

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
import com.example.multinav.model.AudioRecorder
import com.example.multinav.model.bluetooth_service.BluetoothService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.here.sdk.core.engine.AuthenticationMode
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

       // WindowCompat.setDecorFitsSystemWindows(window, true)

        checkAndRequestPermissions()
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()

// Initialize HERE SDK with accessKeyId and accessKeySecret
        val accessKeyId = getString(R.string.here_access_key_id)
        val accessKeySecret = getString(R.string.here_access_key_secret)
        val authenticationMode = AuthenticationMode.withKeySecret(accessKeyId, accessKeySecret)
        val sdkOptions = SDKOptions(authenticationMode).apply {
            cachePath = "${filesDir}/here_sdk_cache"
        }
        try {
            SDKNativeEngine.makeSharedInstance(this, sdkOptions)
            Log.i("MainActivity", "HERE SDK initialized successfully")
        } catch (e: InstantiationErrorException) {
            Log.e("MainActivity", "Failed to initialize HERE SDK: ${e.message}")
            throw RuntimeException("HERE SDK initialization failed", e)
        }

        val user = FirebaseAuth.getInstance().currentUser
//         val email =user?.email
//        val Uid = user?.uid
//        Log.d("email","Email : $email")
//        Log.d("email","uid : $Uid")

        setContent {
            MultiNavTheme {
                Navigation(
                    bluetoothViewModel = bluetoothViewModel,
                    database = database,
                    auth = auth,
                    startDestination =Screen.Splash.route // Start with ActionsAndDelaysScreen for testing
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