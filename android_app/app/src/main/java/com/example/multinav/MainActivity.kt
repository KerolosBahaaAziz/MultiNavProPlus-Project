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
import android.content.pm.PackageManager
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.multinav.bluetooth.BluetoothViewModel
import com.example.multinav.bluetooth.BluetoothViewModelFactory
import com.example.multinav.chat.ChatScreen
import com.example.multinav.chat.ChatViewModel
import com.example.multinav.chat.ChatViewModelFactory
import com.example.multinav.model.AudioRecorder
import com.example.multinav.model.bluetooth_service.BluetoothService
import com.example.multinav.model.paypal.PayPalPaymentManager
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
            deviceAddress = null.toString(),
            bluetoothService = bluetoothService,
            audioRecorder = audioRecorder
        )
    }

    private var showPermissionDialog by mutableStateOf(false)
    private var showDeniedDialog by mutableStateOf(false)
    private var permissionsChecked by mutableStateOf(false)
    private var permissionsHandled by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            bluetoothViewModel.startInitialBleScan()
            Log.i("TAG", "All permissions granted")
        } else {
            // Show denied dialog
            showDeniedDialog = true
        }
        permissionsHandled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle PayPal return on initial launch
        handlePayPalReturn(intent)

        // Initialize Firebase and HERE SDK early
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()

        initializeHereSDK()

        val startDestination = if (auth.currentUser != null && auth.currentUser!!.isEmailVerified) {
            Screen.DeviceList.route
        } else {
            Screen.Login.route
        }

        setContent {
            MultiNavTheme {
                // Check permissions on first composition
                LaunchedEffect(Unit) {
                    if (!permissionsChecked) {
                        permissionsChecked = true
                        checkPermissions()
                    }
                }

                // Show permission dialog if needed
                if (showPermissionDialog) {
                    PermissionExplanationDialog(
                        onGrantClick = {
                            showPermissionDialog = false
                            requestPermissions()
                        },
                        onContinueClick = {
                            showPermissionDialog = false
                            permissionsHandled = true
                        }
                    )
                }

                // Show denied dialog if needed
                if (showDeniedDialog) {
                    PermissionDeniedDialog(
                        onRetryClick = {
                            showDeniedDialog = false
                            showPermissionDialog = true
                        },
                        onContinueClick = {
                            showDeniedDialog = false
                        }
                    )
                }

                // Show navigation when permissions check is complete
                if (permissionsHandled) {
                    Navigation(
                        bluetoothViewModel = bluetoothViewModel,
                        database = database,
                        auth = auth,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    @Composable
    fun PermissionExplanationDialog(
        onGrantClick: () -> Unit,
        onContinueClick: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Permissions Needed") },
            text = {
                Text(
                    "This app works best with the following permissions:\n\n" +
                            "• Bluetooth: To connect and communicate with Bluetooth devices\n" +
                            "• Location: Required for Bluetooth scanning on Android\n" +
                            "• Microphone: To record audio for voice messages\n\n" +
                            "You can grant these permissions now or continue without them (some features may be limited)."
                )
            },
            confirmButton = {
                TextButton(onClick = onGrantClick) {
                    Text("Grant Permissions")
                }
            },
           
        )
    }

    @Composable
    fun PermissionDeniedDialog(
        onRetryClick: () -> Unit,
        onContinueClick: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Permissions Not Granted") },
            text = {
                Text(
                    "Some permissions were denied. " +
                            "Bluetooth features will not be available without these permissions. " +
                            "You can try again or continue with limited functionality."
                )
            },
            confirmButton = {
                TextButton(onClick = onRetryClick) {
                    Text("Try Again")
                }
            },
            dismissButton = {
                TextButton(onClick = onContinueClick) {
                    Text("Continue Anyway")
                }
            }
        )
    }

    private fun checkPermissions() {
        val permissions = getRequiredPermissions()

        // Check if all permissions are already granted
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            // All permissions already granted
            permissionsHandled = true
            bluetoothViewModel.startInitialBleScan()
        } else {
            // Show permission explanation dialog
            showPermissionDialog = true
        }
    }

    private fun getRequiredPermissions(): List<String> {
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

        return permissions
    }

    private fun requestPermissions() {
        val permissions = getRequiredPermissions()
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun initializeHereSDK() {
        // Force English locale before SDK initialization
        val locale = java.util.Locale("en", "US")
        java.util.Locale.setDefault(locale)

        val accessKeyId = getString(R.string.here_access_key_id)
        val accessKeySecret = getString(R.string.here_access_key_secret)
        val authenticationMode = AuthenticationMode.withKeySecret(accessKeyId, accessKeySecret)
        val sdkOptions = SDKOptions(authenticationMode).apply {
            cachePath = "${filesDir}/here_sdk_cache"
            // Try to set any available language options
            try {
                // Some versions support these properties
                val field = this::class.java.getDeclaredField("languageCode")
                field.isAccessible = true
                field.set(this, "en")
                Log.d("MainActivity", "Set languageCode via reflection")
            } catch (e: Exception) {
                Log.d("MainActivity", "languageCode field not available: ${e.message}")
            }
        }

        try {
            val sdkEngine = SDKNativeEngine.makeSharedInstance(this, sdkOptions)
            Log.i("MainActivity", "HERE SDK initialized successfully with locale: $locale")
        } catch (e: InstantiationErrorException) {
            Log.e("MainActivity", "Failed to initialize HERE SDK: ${e.message}")
            throw RuntimeException("HERE SDK initialization failed", e)
        }
    }
    override fun attachBaseContext(newBase: android.content.Context) {
        // Force English locale for the entire app
        val locale = java.util.Locale.ENGLISH
        java.util.Locale.setDefault(locale)

        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    // Handle new intents when app is already running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent?.let {
            handlePayPalReturn(it)
            setIntent(it)
        }
    }

    // Handle PayPal return deep links
    private fun handlePayPalReturn(intent: Intent) {
        val data = intent.data
        Log.d("PayPal", "Received intent with data: $data")

        if (data != null && data.scheme == "com.example.multinav" && data.host == "paypal") {
            when (data.path) {
                "/success" -> {
                    val token = data.getQueryParameter("token")
                    val payerId = data.getQueryParameter("PayerID")
                    Log.d("PayPal", "Payment success - Token: $token, PayerID: $payerId")
                    PayPalPaymentManager.onPaymentSuccess(token, payerId)
                }
                "/cancel" -> {
                    Log.d("PayPal", "Payment cancelled")
                    PayPalPaymentManager.onPaymentCancelled()
                }
            }
            intent.data = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.disconnect()
        SDKNativeEngine.getSharedInstance()?.dispose()
    }
}