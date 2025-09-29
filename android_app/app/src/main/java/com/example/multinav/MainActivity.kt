@file:Suppress("DEPRECATION")

package com.example.multinav

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.multinav.ui.theme.MultiNavTheme
import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import com.example.multinav.bluetooth.BluetoothViewModel
import com.example.multinav.bluetooth.BluetoothViewModelFactory
import com.example.multinav.chat.ChatScreen
import com.example.multinav.chat.ChatViewModel
import com.example.multinav.chat.ChatViewModelFactory
import com.example.multinav.chat.Message
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
            deviceAddress = null.toString(), // Will be set when navigating to ChatScreen
            bluetoothService = bluetoothService,
            audioRecorder = audioRecorder
        )
    }
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            bluetoothViewModel.startInitialBleScan()
            Log.i("TAG", "permissions checked")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //  WindowCompat.setDecorFitsSystemWindows(window,false)
        checkAndRequestPermissions()

        // Handle PayPal return on initial launch
        handlePayPalReturn(intent)

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
        val startDestination = if (auth.currentUser != null && auth.currentUser!!.isEmailVerified) {
            Screen.DeviceList.route // Navigate to main screen if signed in and email verified
        } else {
            Screen.Login.route // Navigate to login screen otherwise
        }

        setContent {
            MultiNavTheme {
                Navigation(
                    bluetoothViewModel = bluetoothViewModel,
                    database = database,
                    auth = auth,
                    startDestination = startDestination
                )
            }
        }
    }

    // Handle new intents when app is already running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent?.let {
            handlePayPalReturn(it)
            setIntent(it) // Update the intent
        }
    }

    // Handle PayPal return deep links
    private fun handlePayPalReturn(intent: Intent) {
        val data = intent.data
        Log.d("PayPal", "Received intent with data: $data")

        if (data != null && data.scheme == "com.example.multinav" && data.host == "paypal") {
            when (data.path) {
                "/success" -> {
                    // Extract query parameters
                    val token = data.getQueryParameter("token") // PayPal order ID
                    val payerId = data.getQueryParameter("PayerID")

                    Log.d("PayPal", "Payment success - Token: $token, PayerID: $payerId")

                    // Notify the payment manager
                    PayPalPaymentManager.onPaymentSuccess(token, payerId)
                }
                "/cancel" -> {
                    Log.d("PayPal", "Payment cancelled")
                    PayPalPaymentManager.onPaymentCancelled()
                }
            }

            // Clear the intent data to prevent re-processing
            intent.data = null
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