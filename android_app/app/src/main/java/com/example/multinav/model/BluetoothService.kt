package com.example.multinav.model

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.multinav.chat.Message
import com.example.multinav.utils.ByteUtils
import com.example.multinav.utils.ByteUtils.bytesToFloats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException


class BluetoothService(private val context: Context) {
    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val prefs = context.getSharedPreferences("BLEPrefs", Context.MODE_PRIVATE)


    private var gattServer: BluetoothGattServer? = null
    private var gattClient: BluetoothGatt? = null

    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    // Added: Variables for voice characteristics to support voice messaging
    private var voiceWriteCharacteristic: BluetoothGattCharacteristic? = null
    private var voiceNotifyCharacteristic: BluetoothGattCharacteristic? = null

    private var isScanning = false
    private var isAdvertising = false

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectionStatus =
        MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    // Add Bluetooth state flow
    private val _bluetoothState = MutableStateFlow(isBluetoothEnabled())
    val bluetoothState: StateFlow<Boolean> = _bluetoothState

    // Store messages per device address
    private val _messagesPerDevice = ConcurrentHashMap<String, MutableList<Message>>()
    private val _messagesFlow = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val messagesFlow: StateFlow<Map<String, List<Message>>> = _messagesFlow.asStateFlow()

    private var messageListener: ((List<Float>, String) -> Unit)? = null // (floats, deviceAddress)
    private var voiceMessageListener: ((ByteArray, String) -> Unit)? = null

    private val scanResults = mutableMapOf<String, BluetoothDevice>()

    private var lastConnectionStateChange = 0L
    private val connectionStateDebounceMs = 1000L // 1 second debounce

    // Added: Variables for debouncing notifications
    private var lastMessageReceivedTime = 0L
    private var lastMessageReceived: String? = null
    private val messageDebounceMs = 500L // 500ms debounce for notifications

    private val deviceNameToAddressMap = mutableMapOf<String, String>()
    private var isReceiverRegistered = false
    private var leScanCallback: ScanCallback? = null

    private val _bleModuleScannedDevices = MutableStateFlow<List<BluetoothDeviceData>>(emptyList())
    val bleModuleScannedDevices: StateFlow<List<BluetoothDeviceData>> = _bleModuleScannedDevices.asStateFlow()

    private val _scannedDevicesList = MutableStateFlow<List<BluetoothDeviceData>>(emptyList())
    val scannedDevicesList: StateFlow<List<BluetoothDeviceData>> = _scannedDevicesList.asStateFlow()

    // Request the BLE module to scan for devices
    @SuppressLint("MissingPermission")
    suspend fun requestBleModuleScan(): Boolean {
        if (!_isConnected.value) {
            Log.e("BLE", "Cannot request scan: Not connected to BLE module")
            return false
        }

        // Clear previous device list
        _scannedDevicesList.value = emptyList()

        // For BLE PRO V2, we need to:
        // 1. Get the B_STATE characteristic
        val bStateService = gattClient?.getService(BLEConfig.BLIST_CONNECTION_SERVICE_UUID)
        val bStateChar = bStateService?.getCharacteristic(BLEConfig.B_STATE_CHARACTERISTIC_UUID)

        if (bStateChar == null) {
            Log.e("BLE", "B_STATE characteristic not found")
            return false
        }

        // 2. Write 'c' to start scanning
        bStateChar.value = "c".toByteArray(Charsets.UTF_8)
        val writeSuccess = gattClient?.writeCharacteristic(bStateChar) ?: false

        if (!writeSuccess) {
            Log.e("BLE", "Failed to send scan command to BLE module")
            return false
        }

        Log.d("BLE", "Sent scan command 'c' to BLE module")
        return true
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDeviceByIndex(index: Int): Boolean {
        if (!_isConnected.value) {
            Log.e("BLE", "Cannot connect: Not connected to BLE module")
            return false
        }

        // Validate index
        if (index < 0 || index >= _scannedDevicesList.value.size) {
            Log.e("BLE", "Invalid device index: $index")
            return false
        }

        // For BLE PRO V2, we need to:
        // 1. Get the B_STATE characteristic
        val bStateService = gattClient?.getService(BLEConfig.BLIST_CONNECTION_SERVICE_UUID)
        val bStateChar = bStateService?.getCharacteristic(BLEConfig.B_STATE_CHARACTERISTIC_UUID)

        if (bStateChar == null) {
            Log.e("BLE", "B_STATE characteristic not found for device selection")
            return false
        }

        // 2. Write the index as a string (per the spec)
        val indexCommand = index.toString()
        bStateChar.value = indexCommand.toByteArray(Charsets.UTF_8)
        val writeSuccess = gattClient?.writeCharacteristic(bStateChar) ?: false

        if (!writeSuccess) {
            Log.e("BLE", "Failed to send device index $index to BLE module")
            return false
        }

        Log.d("BLE", "Sent device index $index to BLE module")

        // Give the BLE module time to process the connection
        delay(1000)

        return true
    }

    // Helper function to get the key for a device (name or address if name is null/blank)
    @SuppressLint("MissingPermission")
    private fun getDeviceKey(device: BluetoothDevice): String {
        val name = device.name
        val address = device.address
        return if (name != null && name.isNotBlank()) {
            if (deviceNameToAddressMap.containsKey(name) && deviceNameToAddressMap[name] != address) {
                "$name ($address)"
            } else {
                deviceNameToAddressMap[name] = address
                name
            }
        } else {
            address
        }
    }

    val connectedDeviceName: String?
        get() = gattClient?.device?.let { getDeviceKey(it) }

    var isMobileDevice = false


    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            Log.d("BLE", "Bluetooth enabled")
                            _bluetoothState.value = true
                        }

                        BluetoothAdapter.STATE_OFF -> {
                            Log.d("BLE", "Bluetooth disabled")
                            _bluetoothState.value = false
                            disconnect()
                        }
                    }
                }
            }
        }
    }

    init {
        // Register BroadcastReceiver for Bluetooth state changes
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)
        isReceiverRegistered = true
//        // Load messages from SharedPreferences
//        val savedMessagesJson = prefs.getString("messagesPerDevice", null)
//        if (savedMessagesJson != null) {
//            val type = object : TypeToken<Map<String, MutableList<Message>>>() {}.type
//            _messagesPerDevice.putAll(gson.fromJson(savedMessagesJson, type))
//            _messagesFlow.value = _messagesPerDevice.toMap()
//        }
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun enableLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        ContextCompat.startActivity(context, intent, null)
    }

    // Start advertising to be discovered
    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        if (isAdvertising) return

        try {
            // Check if Bluetooth is enabled
            if (bluetoothAdapter?.isEnabled != true) {
                Log.e("BLE", "Bluetooth is not enabled")
                _connectionStatus.value = ConnectionStatus.Error("Bluetooth is not enabled")
                return
            }

            // Start GATT server first before advertising
            startGattServer()

            val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
            if (advertiser == null) {
                Log.e("BLE", "Bluetooth LE Advertiser not available")
                _connectionStatus.value = ConnectionStatus.Error("BLE Advertiser not available")
                return
            }

            // Create minimal advertising data
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED) // Changed to BALANCED
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW) // Changed to LOW
                .setConnectable(true)
                .build()

            // Minimal advertising data - just the service UUID
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(BLEConfig.CHAT_SERVICE_UUID))
                .build()

            // Use a safe callback
            val safeCallback = SafeAdvertiseCallback()

            // Start advertising with try-catch
            try {
                advertiser.startAdvertising(settings, data, safeCallback)
                isAdvertising = true
                Log.d("BLE", "Started advertising with minimal data")
            } catch (e: Exception) {
                isAdvertising = false
                Log.e("BLE", "Exception starting advertising", e)
                _connectionStatus.value =
                    ConnectionStatus.Error("Failed to start advertising: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("BLE", "Exception in startAdvertising", e)
            _connectionStatus.value =
                ConnectionStatus.Error("Error in advertising setup: ${e.message}")
        }
    }

    // Safe callback that won't crash
    private inner class SafeAdvertiseCallback : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            try {
                Log.d("BLE", "Advertising started")
                isAdvertising = true
                // Don't start GATT server here, it's already started
            } catch (e: Exception) {
                Log.e("BLE", "Exception in onStartSuccess", e)
                _connectionStatus.value =
                    ConnectionStatus.Error("Error after advertising started: ${e.message}")
            }
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            try {
                val errorMessage = when (errorCode) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertising data too large"
                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                    ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already started"
                    ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal advertising error"
                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Advertising not supported"
                    else -> "Unknown advertising error: $errorCode"
                }
                Log.e("BLE", "Advertising failed: $errorMessage")
                _connectionStatus.value = ConnectionStatus.Error(errorMessage)
            } catch (e: Exception) {
                Log.e("BLE", "Exception in onStartFailure", e)
                _connectionStatus.value =
                    ConnectionStatus.Error("Error handling advertising failure")
            }
        }
    }

    // Scan for devices
    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (isScanning) return

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BLEConfig.CHAT_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED) // Changed to BALANCED
            .build()

        scanner?.startScan(listOf(scanFilter), settings, scanCallback)
        isScanning = true
        Log.d("BLE", "Started scanning")
    }


    // Stop scanning
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            Log.d("BLE", "Stopped scanning")
        } catch (e: Exception) {
            Log.e("BLE", "Error stopping scanning", e)
        }
    }

    // Stop advertising
    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        if (!isAdvertising) return
        try {
            bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(SafeAdvertiseCallback())
            isAdvertising = false
            Log.d("BLE", "Stopped advertising")
        } catch (e: Exception) {
            Log.e("BLE", "Error stopping advertising", e)
        }
    }

    // Scan callback
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            scanResults[device.address] = device
            Log.d("BLE", "Found device: ${device.name ?: "Unknown"} (${device.address})")
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE", "Scan failed with error: $errorCode")
            isScanning = false
            _connectionStatus.value = ConnectionStatus.Error("Scanning failed: $errorCode")
        }
    }

    // Start GATT server
    @SuppressLint("MissingPermission")
    private fun startGattServer() {
        try {
            gattServer?.close()
            gattServer = null

            gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)

            if (gattServer == null) {
                Log.e("BLE", "Failed to open GATT server")
                _connectionStatus.value = ConnectionStatus.Error("Failed to open GATT server")
                return
            }

            // Add mobile service
            val mobileService = BLEConfig.createChatService()
            writeCharacteristic =
                mobileService.getCharacteristic(BLEConfig.WRITE_CHARACTERISTIC_UUID)
            notifyCharacteristic =
                mobileService.getCharacteristic(BLEConfig.NOTIFY_CHARACTERISTIC_UUID)
            // Added: Initialize voice characteristics for mobile service
            voiceWriteCharacteristic =
                mobileService.getCharacteristic(BLEConfig.VOICE_WRITE_CHARACTERISTIC_UUID)
            voiceNotifyCharacteristic =
                mobileService.getCharacteristic(BLEConfig.VOICE_NOTIFY_CHARACTERISTIC_UUID)
            if (writeCharacteristic == null || notifyCharacteristic == null || voiceWriteCharacteristic == null || voiceNotifyCharacteristic == null) {
                Log.e("BLE", "Failed to initialize mobile characteristics")
                _connectionStatus.value =
                    ConnectionStatus.Error("Failed to initialize mobile characteristics")
                gattServer?.close()
                gattServer = null
                return
            }
            val mobileSuccess = gattServer?.addService(mobileService) ?: false

            // Add BLE service
            val bleService = BLEConfig.createBLEChatService()
            val bleWriteCharacteristic =
                bleService.getCharacteristic(BLEConfig.BLE_WRITE_CHARACTERISTIC_UUID)
            val bleNotifyCharacteristic =
                bleService.getCharacteristic(BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID)
            if (bleWriteCharacteristic == null || bleNotifyCharacteristic == null) {
                Log.e("BLE", "Failed to initialize BLE characteristics")
                _connectionStatus.value =
                    ConnectionStatus.Error("Failed to initialize BLE characteristics")
                gattServer?.close()
                gattServer = null
                return
            }
            val bleSuccess = gattServer?.addService(bleService) ?: false

            if (mobileSuccess && bleSuccess) {
                Log.d("BLE", "GATT Server started successfully with mobile and BLE services")
                Log.d(
                    "BLE",
                    "Mobile Write: ${writeCharacteristic?.uuid}, Notify: ${notifyCharacteristic?.uuid}"
                )
                Log.d(
                    "BLE",
                    "BLE Write: ${bleWriteCharacteristic?.uuid}, Notify: ${bleNotifyCharacteristic?.uuid}"
                )
                Log.d(
                    "BLE",
                    "Voice Write: ${voiceWriteCharacteristic?.uuid}, Voice Notify: ${voiceNotifyCharacteristic?.uuid}"
                )
            } else {
                Log.e("BLE", "Failed to add services to GATT Server")
                _connectionStatus.value =
                    ConnectionStatus.Error("Failed to add services to GATT Server")
                gattServer?.close()
                gattServer = null
            }
        } catch (e: Exception) {
            Log.e("BLE", "Exception in startGattServer", e)
            _connectionStatus.value =
                ConnectionStatus.Error("Error starting GATT server: ${e.message}")
            try {
                gattServer?.close()
                gattServer = null
            } catch (e2: Exception) {
                Log.e("BLE", "Exception cleaning up GATT server", e2)
            }
        }
    }


    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(
        address: String,
        isMobileDevice: Boolean,
        attempt: Int = 1,
        maxAttempts: Int = 3
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (attempt > maxAttempts) {
                    _connectionStatus.value =
                        ConnectionStatus.Error("Max connection attempts reached")
                    return@withContext false
                }

                // Disconnect from any existing connection before connecting to a new device
                if (_isConnected.value) {
                    disconnect()
                    // Add a small delay to ensure the disconnection completes
                    delay(500)
                }

                stopScanning() // Use the existing stopScanning method
                stopAdvertising()
                _connectionStatus.value = ConnectionStatus.Connecting
                val device = bluetoothAdapter?.getRemoteDevice(address)
                if (device == null) {
                    _connectionStatus.value = ConnectionStatus.Error("Device not found")
                    Log.e("BLE", "Device not found for address: $address")
                    return@withContext false
                }

                try {
                    gattClient?.close()
                    gattClient = null
                } catch (e: Exception) {
                    Log.e("BLE", "Error closing existing GATT client", e)
                }
                gattClient = device.connectGatt(
                    context,
                    false,
                    gattClientCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
                if (gattClient == null) {
                    _connectionStatus.value = ConnectionStatus.Error("Failed to create GATT client")
                    Log.e("BLE", "Failed to create GATT client for $address")
                    return@withContext false
                }
                var timeout = 0
                while (!_isConnected.value && timeout < 100) {
                    delay(100)
                    timeout++
                    ensureActive()
                }
                if (!_isConnected.value) {
                    _connectionStatus.value = ConnectionStatus.Error("Connection timeout")
                    delay(2000L * attempt)
                    return@withContext connectToDevice(
                        address,
                        isMobileDevice,
                        attempt + 1,
                        maxAttempts
                    )
                }
                saveLastConnectedDevice(address, isMobileDevice)

                if (!_messagesPerDevice.containsKey(address)) {
//                    _messagesPerDevice[address] =
//                        mutableListOf  (   Message.Text("Welcome to Bluetooth Chat", false))
                    _messagesFlow.value = _messagesPerDevice.toMap()
                }

                return@withContext true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("BLE", "Connection error on attempt $attempt", e)
                _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Unknown error")
                delay(2000L * attempt)
                return@withContext connectToDevice(
                    address,
                    isMobileDevice,
                    attempt + 1,
                    maxAttempts
                )
            }
        }
    }

    private fun saveLastConnectedDevice(address: String, isMobileDevice: Boolean) {
        val prefs = context.getSharedPreferences("BLEPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("lastConnectedDevice", address)
            .putBoolean("isMobileDevice", isMobileDevice)
            .apply()
        Log.d("BLE", "Saved last connected device: $address, isMobileDevice: $isMobileDevice")
    }

    @SuppressLint("MissingPermission")
    suspend fun sendMessage(message: String, isMobileDevice: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (gattClient != null) {
                    val success = sendAsClient(message, isMobileDevice)
                    if (success) {
                        Log.d("BLE", "Message sent as client: $message")
                        // Removed the code that adds the message to _messagesPerDevice, since ChatViewModel already does this
                        return@withContext true
                    }
                }

                if (!_isConnected.value) {
                    Log.e("BLE", "Cannot send message: Not connected")
                    return@withContext false
                }

                Log.e("BLE", "Failed to send message: No valid connection")
                return@withContext false
            } catch (e: Exception) {
                Log.e("BLE", "Send message error", e)
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLeScan(onDeviceFound: (BluetoothDevice, Int, Boolean) -> Unit) {
        if (isScanning) return

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        // Temporarily remove scan filters to debug
        val scanFilters = emptyList<ScanFilter>() // No filters, scan for all devices

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val rssi = result.rssi
                // Log all service UUIDs to debug
                val serviceUuids = result.scanRecord?.serviceUuids?.joinToString() ?: "None"
                Log.d(
                    "BLE",
                    "Found device: ${device.name} (${device.address}), RSSI: $rssi, Service UUIDs: $serviceUuids"
                )
                // Still classify based on expected UUIDs for UI purposes
                val isMobileDevice =
                    result.scanRecord?.serviceUuids?.contains(ParcelUuid(BLEConfig.CHAT_SERVICE_UUID)) == true
                onDeviceFound(device, rssi, isMobileDevice)
            }
        }
        scanner?.startScan(scanFilters, settings, scanCallback)
        isScanning = true
        Log.d("BLE", "Started scanning with no filters for debugging")
    }


    @SuppressLint("MissingPermission")
    private fun sendAsServer(message: String, isMobileDevice: Boolean): Boolean {
        // Choose the correct notify characteristic based on isMobileDevice
        val notifyChar = if (isMobileDevice) {
            notifyCharacteristic
        } else {
            gattServer?.getService(BLEConfig.BLE_SERVICE_UUID)
                ?.getCharacteristic(BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID)
        }

        notifyChar?.let { characteristic ->
            try {
                characteristic.value = message.toByteArray()
                val connectedDevices =
                    bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT_SERVER)
                        ?: emptyList()
                if (connectedDevices.isEmpty()) {
                    Log.e("BLE", "No connected devices found for server send operation")
                    return false
                }
                Log.d("BLE", "Found ${connectedDevices.size} connected device(s) for server send")

                var success = false
                for (device in connectedDevices) {
                    val result =
                        gattServer?.notifyCharacteristicChanged(device, characteristic, false)
                            ?: false
                    success = success || result
                    Log.d(
                        "BLE",
                        "Server sent message: $message to ${device.address}, success: $result"
                    )
                }
                if (!success) {
                    Log.e("BLE", "Failed to notify any connected device of message: $message")
                }
                return success
            } catch (e: Exception) {
                Log.e("BLE", "Error in sendAsServer while notifying", e)
                return false
            }
        }
        Log.e("BLE", "Notify characteristic not set for server send")
        return false
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendAsClient(message: String, isMobileDevice: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            val serviceUuid =
                if (isMobileDevice) BLEConfig.CHAT_SERVICE_UUID else BLEConfig.BLE_SERVICE_UUID
            val writeUuid =
                if (isMobileDevice) BLEConfig.WRITE_CHARACTERISTIC_UUID else BLEConfig.BLE_WRITE_CHARACTERISTIC_UUID

            Log.d(
                "msg",
                "sendAsClient: message='$message', isMobileDevice=$isMobileDevice, serviceUuid=$serviceUuid, writeUuid=$writeUuid"
            )

            val service = gattClient?.getService(serviceUuid)
            if (service == null) {
                Log.e("msg", "Chat service not found for client send (UUID: $serviceUuid)")
                return@withContext false
            }

            val characteristic = service.getCharacteristic(writeUuid)
            if (characteristic == null) {
                Log.e("msg", "Write characteristic not found for client send (UUID: $writeUuid)")
                return@withContext false
            }

            try {
                characteristic.writeType = if (isMobileDevice) {
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                } else {
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                }
                Log.d("msg", "Set writeType to ${characteristic.writeType} for message: $message")

                val bytes = message.toByteArray(Charsets.UTF_8)
                Log.d(
                    "msg",
                    "Converted message to bytes: ${bytes.joinToString()}, length: ${bytes.size}"
                )

                characteristic.value = bytes
                val success = gattClient?.writeCharacteristic(characteristic) ?: false
                if (success) {
                    Log.d(
                        "BLE",
                        "Client sent message: '$message' to $serviceUuid, success: $success"
                    )
                    if (isMobileDevice) {
                        var timeout = 0
                        while (timeout < 50) { // Wait up to 5 seconds
                            delay(100)
                            timeout++
                            if (characteristic.value?.contentEquals(bytes) == true) {
                                Log.d(
                                    "BLE",
                                    "Write operation confirmed successful for message: '$message'"
                                )
                                return@withContext true
                            }
                        }
                        Log.e("BLE", "Write operation timed out for message: '$message'")
                        return@withContext false
                    }
                    return@withContext true
                } else {
                    Log.e("BLE", "Failed to send message: '$message' to $serviceUuid")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e("msg", "Error sending message as client: '$message'", e)
                return@withContext false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        // Check client connection
        if (_isConnected.value && gattClient?.device?.address == device.address) {
            return true
        }

        // Check server connections using BluetoothManager instead of GattServer
        val connectedDevices =
            bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT_SERVER) ?: emptyList()
        return connectedDevices.any { it.address == device.address }
    }

    // GATT server callback
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.d("BLE", "Server connection state change: $status -> $newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _isConnected.value = true
                    _connectionStatus.value = ConnectionStatus.Connected
                    Log.d("BLE", "Device connected to server: ${device.address}")
                    device.address?.let { deviceAddress ->
                        if (!_messagesPerDevice.containsKey(deviceAddress)) {
//                            _messagesPerDevice[deviceAddress] =
//                                mutableListOf(Message.Text("Welcome to Bluetooth Chat", false))
                            _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }
                        }
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    _isConnected.value = false
                    _connectionStatus.value = ConnectionStatus.Disconnected
                    Log.d("BLE", "Device disconnected from server: ${device.address}")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            Log.d("BLE", "Server received write request for ${characteristic.uuid}")
            device.address?.let { deviceAddress ->
                // Handle text messages
                if (characteristic.uuid == BLEConfig.WRITE_CHARACTERISTIC_UUID) {
                    // Log the raw bytes as hex for debugging
                    val hexMessage = ByteUtils.bytesToHex(value)
                    Log.d(
                        "BLE",
                        "Server received raw message (hex): $hexMessage from ${device.address}"
                    )
                    // Convert the raw bytes to floats
                    val floats = bytesToFloats(value)
                    val displayMessage = "Floats: [${floats.joinToString(", ")}]"
                    // Add the formatted float message to the device's message list
                    val messages = _messagesPerDevice.getOrPut(deviceAddress) { mutableListOf() }
                    messages.add(
                        Message.Text(
                            displayMessage,
                            false
                        )
                    ) // Fixed: Use Message.Text instead of Message
                    _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }
                    // Invoke the message listener with the float values and device address
                    messageListener?.let { listener ->
                        Log.d("BLE", "Invoking listener with floats for device: $deviceAddress")
                        listener(floats, deviceAddress)
                    } ?: Log.w("BLE", "No listener set for received raw message")
                }
                // Added: Handle voice messages
                else if (characteristic.uuid == BLEConfig.VOICE_WRITE_CHARACTERISTIC_UUID) {
                    Log.d(
                        "BLE",
                        "Server received voice message from ${device.address}, bytes: ${value.size}"
                    )
                    voiceMessageListener?.invoke(value, deviceAddress) ?: Log.w(
                        "BLE",
                        "No listener set for received voice message"
                    )
                } else {

                }
            } ?: Log.e("BLE", "Device address is null, cannot process message")

            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                Log.d("BLE", "Sent response to write request from ${device.address}")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            Log.d("BLE", "Server received descriptor write: ${descriptor.uuid}")
            if (descriptor.uuid == BLEConfig.CLIENT_CONFIG_UUID) {
                Log.d("BLE", "Client enabled notifications: ${device.address}")
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            }
        }
    }

    // GATT client callback
    private val gattClientCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastConnectionStateChange < connectionStateDebounceMs) {
                    Log.d("BLE", "Ignoring rapid connection state change: $status -> $newState")
                    return
                }
                lastConnectionStateChange = currentTime

                Log.d(
                    "BLE",
                    "Client connection state change: $status -> $newState for ${gatt.device?.address ?: "Unknown"}"
                )
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _isConnected.value = true
                        _connectionStatus.value = ConnectionStatus.Connected

                        // Add a small delay before discovering services
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                if (_isConnected.value && gatt.device != null) {
                                    Log.d(
                                        "BLE",
                                        "Starting service discovery for ${gatt.device.address}"
                                    )
                                    val success = gatt.discoverServices()
                                    Log.d("BLE", "Service discovery initiated: $success")
                                }
                            } catch (e: Exception) {
                                Log.e("BLE", "Error discovering services", e)
                            }
                        }, 500)

                        Log.d("BLE", "Connected to server: ${gatt.device.address}")
                        // Initialize message list for this device if it doesn't exist
                        gatt.device.address?.let { deviceAddress ->
                            if (!_messagesPerDevice.containsKey(deviceAddress)) {
//                                _messagesPerDevice[deviceAddress] =
//                                    mutableListOf(Message.Text("Welcome to Bluetooth Chat", false))
                                _messagesFlow.value =
                                    _messagesPerDevice.mapValues { it.value.toList() }
                            }
                        }
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _isConnected.value = false
                        _connectionStatus.value = ConnectionStatus.Disconnected
                        Log.d(
                            "BLE",
                            "Disconnected from server: ${gatt.device?.address ?: "Unknown"}"
                        )
                        // Added: Reset characteristics on disconnect
                        writeCharacteristic = null
                        notifyCharacteristic = null
                        voiceWriteCharacteristic = null
                        voiceNotifyCharacteristic = null
                    }
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error in onConnectionStateChange", e)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLE", "Services discovered:")
                    gatt.services.forEach { service ->
                        Log.d("BLE", "Service: ${service.uuid}")
                        service.characteristics.forEach { characteristic ->
                            Log.d("BLE", "  Characteristic: ${characteristic.uuid}")
                        }
                    }

                    val serviceUuid =
                        if (isMobileDevice) BLEConfig.CHAT_SERVICE_UUID else BLEConfig.BLE_SERVICE_UUID
                    val notifyUuid =
                        if (isMobileDevice) BLEConfig.NOTIFY_CHARACTERISTIC_UUID else BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID
                    // Added: UUIDs for voice characteristics
                    val voiceNotifyUuid =
                        if (isMobileDevice) BLEConfig.VOICE_NOTIFY_CHARACTERISTIC_UUID else BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID
                    val service = gatt.getService(serviceUuid)

                    if (service != null) {
                        writeCharacteristic = service.getCharacteristic(
                            if (isMobileDevice) BLEConfig.WRITE_CHARACTERISTIC_UUID else BLEConfig.BLE_WRITE_CHARACTERISTIC_UUID
                        )
                        notifyCharacteristic = service.getCharacteristic(notifyUuid)
                        // Added: Initialize voice characteristics
                        voiceWriteCharacteristic = service.getCharacteristic(
                            if (isMobileDevice) BLEConfig.VOICE_WRITE_CHARACTERISTIC_UUID else BLEConfig.BLE_WRITE_CHARACTERISTIC_UUID
                        )
                        voiceNotifyCharacteristic = service.getCharacteristic(voiceNotifyUuid)

                        Log.d(
                            "BLE",
                            "Chat service found (UUID: $serviceUuid). Write: ${writeCharacteristic?.uuid}, Notify: ${notifyCharacteristic?.uuid}"
                        )
                        Log.d(
                            "BLE",
                            "Voice Write: ${voiceWriteCharacteristic?.uuid}, Voice Notify: ${voiceNotifyCharacteristic?.uuid}"
                        )

                        if (notifyCharacteristic != null) {
                            // Enable notifications immediately
                            val success =
                                gatt.setCharacteristicNotification(notifyCharacteristic, true)
                            Log.d("BLE", "Set characteristic notification: $success")
                            if (success) {
                                enableNotifications(gatt, notifyCharacteristic)
                            } else {
                                Log.e("BLE", "Failed to enable notifications at GATT level")
                                retryEnableNotifications(gatt, notifyCharacteristic!!, attempt = 1)
                            }
                        } else {
                            Log.e("BLE", "Notify characteristic not found (UUID: $notifyUuid)")
                            _connectionStatus.value =
                                ConnectionStatus.Error("Notify characteristic not found")
                        }

                        // Added: Enable notifications for voice characteristic
                        if (voiceNotifyCharacteristic != null) {
                            val success =
                                gatt.setCharacteristicNotification(voiceNotifyCharacteristic, true)
                            Log.d("BLE", "Set voice characteristic notification: $success")
                            if (success) {
                                enableNotifications(gatt, voiceNotifyCharacteristic)
                            } else {
                                Log.e("BLE", "Failed to enable voice notifications at GATT level")
                                retryEnableNotifications(
                                    gatt,
                                    voiceNotifyCharacteristic!!,
                                    attempt = 1
                                )
                            }
                        } else {
                            Log.e(
                                "BLE",
                                "Voice notify characteristic not found (UUID: $voiceNotifyUuid)"
                            )
                            _connectionStatus.value =
                                ConnectionStatus.Error("Voice notify characteristic not found")
                        }
                    } else {
                        Log.e("BLE", "Chat service not found (UUID: $serviceUuid)")
                        _connectionStatus.value = ConnectionStatus.Error("Chat service not found")
                    }
                } else {
                    Log.e("BLE", "Service discovery failed with status: $status")
                    _connectionStatus.value =
                        ConnectionStatus.Error("Service discovery failed: $status")
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error in onServicesDiscovered", e)
                _connectionStatus.value =
                    ConnectionStatus.Error("Error discovering services: ${e.message}")
            }
        }

        // For Android 13+ (API 33+)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            try {
                handleCharacteristicChanged(gatt, characteristic, value)
            } catch (e: Exception) {
                Log.e("BLE", "Error in onCharacteristicChanged", e)
            }
        }

        // For older Android versions
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            try {
                handleCharacteristicChanged(gatt, characteristic, characteristic.value)
            } catch (e: Exception) {
                Log.e("BLE", "Error in deprecated onCharacteristicChanged", e)
            }
        }

        private fun handleCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            try {
                val deviceAddress = gatt.device?.address ?: "Unknown"
                Log.d("BLE", "Received notification from ${deviceAddress} on ${characteristic.uuid}")

                val hexMessage = ByteUtils.bytesToHex(value)
                Log.d("BLE", "Raw hex data: $hexMessage")

                // Check if this is sensor data from the Sensor Service
                if (characteristic.service.uuid == BLEConfig.SENSOR_SERVICE_UUID) {
                    // Process sensor data
                    processSensorData(characteristic, value, deviceAddress)
                    return
                }

                // Determine which service this characteristic belongs to
                val serviceName = when(characteristic.service.uuid) {
                    BLEConfig.CHAT_SERVICE_UUID -> "Mobile Chat Service"
                    BLEConfig.BLE_SERVICE_UUID -> "BLE Service"
                    else -> "Unknown Service ${characteristic.service.uuid}"
                }

                // Determine expected characteristic UUID based on service
                val expectedUuid = if (characteristic.service.uuid == BLEConfig.CHAT_SERVICE_UUID) {
                    BLEConfig.NOTIFY_CHARACTERISTIC_UUID
                } else {
                    BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID
                }

                // Expected UUID for voice notifications
                val expectedVoiceUuid = if (characteristic.service.uuid == BLEConfig.CHAT_SERVICE_UUID) {
                    BLEConfig.VOICE_NOTIFY_CHARACTERISTIC_UUID
                } else {
                    BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID
                }

                // Process based on which characteristic received the notification
                if (characteristic.uuid == expectedUuid) {
                    // This is a text/data notification
                    try {
                        val message = String(value, Charsets.UTF_8)
                        Log.d("BLE", "Received text message: '$message' from $serviceName")

                        // Check if this is a scan result message
                        if (message.startsWith("SCAN_RESULT:")) {
                            // Parse scan results from BLE module
                            val scanData = message.substring("SCAN_RESULT:".length)
                            processBleModuleScanResult(scanData)
                            Log.d("BLE", "Processed scan results from BLE module")
                        }
                        // Check if this is a raw device list message (without prefix)
                        else if (message.contains(";") && message.contains(",")) {
                            // Possible device list in CSV format without prefix
                            processBleModuleScanResult(message)
                            Log.d("BLE", "Processed raw scan results from BLE module")
                        }
                        // Check if message contains float data (joystick or control data)
                        else if (value.size >= 4) {
                            // Might be float data - convert to floats for processing
                            val floats = bytesToFloats(value)
                            val displayMessage = "Floats: [${floats.joinToString(", ")}]"

                            // Add the formatted float message to the device's message list
                            val messages = _messagesPerDevice.getOrPut(deviceAddress) { mutableListOf() }
                            messages.add(Message.Text(displayMessage, false))
                            _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }

                            // Invoke the message listener with the float values and device address
                            messageListener?.invoke(floats, deviceAddress) ?:
                            Log.w("BLE", "No listener set for received floats on client")
                        }
                        else {
                            // Regular text message
                            val messages = _messagesPerDevice.getOrPut(deviceAddress) { mutableListOf() }
                            messages.add(Message.Text(message, false))
                            _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }
                            Log.d("BLE", "Added text message to chat: '$message'")
                        }
                    } catch (e: Exception) {
                        Log.e("BLE", "Error processing text data", e)

                        // Fallback to processing as raw data
                        val floats = bytesToFloats(value)
                        messageListener?.invoke(floats, deviceAddress)
                    }
                }
                // Voice data handling
                else if (characteristic.uuid == expectedVoiceUuid) {
                    Log.d("BLE", "Received voice message from ${deviceAddress}, bytes: ${value.size}")
                    voiceMessageListener?.invoke(value, deviceAddress) ?:
                    Log.w("BLE", "No listener set for received voice message on client")
                }
                // Unknown characteristic
                else {
                    Log.w("BLE", "Received data on unexpected characteristic: ${characteristic.uuid}")
                    // Try to process as generic data
                    val message = try {
                        String(value, Charsets.UTF_8)
                    } catch (e: Exception) {
                        "Binary data: ${ByteUtils.bytesToHex(value)}"
                    }

                    Log.d("BLE", "Unknown characteristic data: $message")

                    // Check if this might be scan results (even on unexpected characteristic)
                    if (message.contains(";") && message.contains(",")) {
                        try {
                            processBleModuleScanResult(message)
                            Log.d("BLE", "Processed scan results from unexpected characteristic")
                        } catch (e: Exception) {
                            Log.e("BLE", "Failed to process potential scan results", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error handling characteristic change", e)
            }
        }
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            try {
                Log.d("BLE", "Write completed with status: $status for ${characteristic.uuid}")
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e("BLE", "Write failed with status: $status")
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error in onCharacteristicWrite", e)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            try {
                Log.d(
                    "BLE",
                    "Descriptor write completed with status: $status for ${descriptor.uuid}"
                )
                if (descriptor.uuid == BLEConfig.CLIENT_CONFIG_UUID) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d("BLE", "Notifications successfully enabled")
                    } else {
                        Log.e("BLE", "Failed to enable notifications, status: $status")
                        val localNotifyCharacteristic =
                            if (descriptor.characteristic.uuid == BLEConfig.NOTIFY_CHARACTERISTIC_UUID) {
                                notifyCharacteristic
                            } else {
                                voiceNotifyCharacteristic
                            }
                        if (localNotifyCharacteristic != null) {
                            retryEnableNotifications(gatt, localNotifyCharacteristic, attempt = 1)
                        } else {
                            Log.e(
                                "BLE",
                                "Cannot retry enabling notifications: notifyCharacteristic is null"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error in onDescriptorWrite", e)
            }
        }
    }

    private fun processSensorData(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        deviceAddress: String
    ) {
        try {
            // Create a sensor data message to be stored and processed by listeners
            var sensorType = ""
            var sensorValue = ""
            var unit = ""

            when (characteristic.uuid) {
                BLEConfig.GYROSCOPE_CHARACTERISTIC_UUID -> {  // Gyroscope Data
                    sensorType = "GYRO"
                    if (value.size >= 6) {
                        val x = ByteUtils.bytesToShort(value[0], value[1])
                        val y = ByteUtils.bytesToShort(value[2], value[3])
                        val z = ByteUtils.bytesToShort(value[4], value[5])
                        sensorValue = "$x,$y,$z"
                        unit = "raw"
                    }
                }
                BLEConfig.ACCELEROMETER_CHARACTERISTIC_UUID -> {  // Accelerometer Data
                    sensorType = "ACCEL"
                    if (value.size >= 6) {
                        val x = ByteUtils.bytesToShort(value[0], value[1])
                        val y = ByteUtils.bytesToShort(value[2], value[3])
                        val z = ByteUtils.bytesToShort(value[4], value[5])
                        sensorValue = "$x,$y,$z"
                        unit = "raw"
                    }
                }
                BLEConfig.MAGNETOMETER_CHARACTERISTIC_UUID -> {  // Magnetometer Data
                    sensorType = "MAG"
                    if (value.size >= 6) {
                        val x = ByteUtils.bytesToShort(value[0], value[1])
                        val y = ByteUtils.bytesToShort(value[2], value[3])
                        val z = ByteUtils.bytesToShort(value[4], value[5])
                        sensorValue = "$x,$y,$z"
                        unit = "raw"
                    }
                }
                BLEConfig.AIR_PRESSURE_CHARACTERISTIC_UUID -> {  // Air Pressure Data
                    sensorType = "PRESS"
                    if (value.size >= 3) {
                        val rawPressure = ByteUtils.bytesToInt(value[0], value[1], value[2])
                        val pressure = rawPressure / 4098.0f
                        sensorValue = pressure.toInt().toString()
                        unit = "hPa"
                    }
                }
                BLEConfig.TEMPERATURE_CHARACTERISTIC_UUID -> {  // Temperature Data
                    sensorType = "TEMP"
                    if (value.size >= 2) {
                        val rawTemp = ByteUtils.bytesToShort(value[0], value[1])
                        val temperature = (rawTemp / 16383.0f) * 165.0f - 40.0f
                        sensorValue = temperature.toInt().toString()
                        unit = "°C"
                    }
                }
                BLEConfig.HUMIDITY_CHARACTERISTIC_UUID -> {  // Humidity Data
                    sensorType = "HUM"
                    if (value.size >= 2) {
                        val rawHumidity = ByteUtils.bytesToShort(value[0], value[1])
                        val humidity = (rawHumidity / 16383.0f) * 100.0f
                        sensorValue = humidity.toInt().toString()
                        unit = "%"
                    }
                }
                BLEConfig.AIR_QUALITY_CHARACTERISTIC_UUID -> {  // Air Quality Data
                    sensorType = "AQ"
                    // For air quality, implement a simple mapping for demonstration
                    val rawValue = value.firstOrNull()?.toInt() ?: 0
                    sensorValue = when {
                        rawValue < 50 -> "Excellent"
                        rawValue < 100 -> "Good"
                        rawValue < 150 -> "Moderate"
                        rawValue < 200 -> "Poor"
                        else -> "Very Poor"
                    }
                    unit = ""
                }
            }

            // If we processed a recognized sensor
            if (sensorType.isNotEmpty() && sensorValue.isNotEmpty()) {
                // Format a sensor message in a standard format
                val sensorMessage = "SENSOR:$sensorType=$sensorValue;UNIT=$unit"

                // Add the sensor data to the device's message list
                val messages = _messagesPerDevice.getOrPut(deviceAddress) { mutableListOf() }
                messages.add(Message.Text(sensorMessage, false))
                _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }

                Log.d("BLE", "Processed sensor data: $sensorMessage")
            }
        } catch (e: Exception) {
            Log.e("BLE", "Error processing sensor data", e)
        }
    }

    /**
     * Processes scan results received from the BLE module.
     * Expected format: "deviceName1,deviceAddress1;deviceName2,deviceAddress2;..."
     */
    private fun processBleModuleScanResult(scanData: String) {
        try {
            Log.d("BLE", "Processing scan data: $scanData")

            // Create a mutable list to hold the parsed devices
            val devicesList = mutableListOf<BluetoothDeviceData>()

            // Split the scan data by device entries (separated by semicolons)
            val deviceEntries = scanData.split(";")

            for (entry in deviceEntries) {
                // Skip empty entries
                if (entry.isBlank()) continue

                // Each entry should be in format "deviceName,deviceAddress"
                val parts = entry.split(",")
                if (parts.size >= 2) {
                    val deviceName = parts[0].trim()
                    val deviceAddress = parts[1].trim()

                    // Create a BluetoothDeviceData object
                    val deviceInfo = BluetoothDeviceData(
                        address = deviceAddress,
                        name = deviceName.ifBlank { "Unknown Device" },
                        rssi = 0, // RSSI might not be available from module scan
                        isConnected = true, // Assume all returned devices are connectable
                    )

                    devicesList.add(deviceInfo)
                    Log.d("BLE", "Added device from module scan: $deviceName ($deviceAddress)")
                } else {
                    Log.w("BLE", "Invalid device entry format: $entry")
                }
            }

            // Update the flow with the new scan results
            _scannedDevicesList.value = devicesList
            Log.d("BLE", "Updated scan results with ${devicesList.size} devices")
        } catch (e: Exception) {
            Log.e("BLE", "Error processing BLE module scan results", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeToSensorCharacteristics(gatt: BluetoothGatt?) {
        if (gatt == null) {
            Log.e("BLE", "Cannot subscribe to sensors: GATT connection is null")
            return
        }

        // Get the sensor service
        val sensorService = gatt.getService(BLEConfig.SENSOR_SERVICE_UUID)
        if (sensorService == null) {
            Log.e("BLE", "Sensor service not found")
            return
        }

        // List of all sensor characteristics
        val sensorCharacteristics = listOf(
            BLEConfig.GYROSCOPE_CHARACTERISTIC_UUID,
            BLEConfig.ACCELEROMETER_CHARACTERISTIC_UUID,
            BLEConfig.MAGNETOMETER_CHARACTERISTIC_UUID,
            BLEConfig.AIR_PRESSURE_CHARACTERISTIC_UUID,
            BLEConfig.TEMPERATURE_CHARACTERISTIC_UUID,
            BLEConfig.HUMIDITY_CHARACTERISTIC_UUID,
            BLEConfig.AIR_QUALITY_CHARACTERISTIC_UUID
        )

        // Subscribe to each sensor characteristic
        for (uuid in sensorCharacteristics) {
            val characteristic = sensorService.getCharacteristic(uuid)
            if (characteristic != null) {
                val result = gatt.setCharacteristicNotification(characteristic, true)

                // Set the descriptor for notifications
                val descriptor = characteristic.getDescriptor(BLEConfig.CLIENT_CONFIG_UUID)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    Log.d("BLE", "Subscribed to sensor: ${uuid}")
                } else {
                    Log.e("BLE", "Notification descriptor not found for sensor: ${uuid}")
                }
            } else {
                Log.e("BLE", "Sensor characteristic not found: ${uuid}")
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic?
    ) {
        if (characteristic == null) {
            Log.e("BLE", "Characteristic is null, cannot enable notifications")
            return
        }
        try {
            val descriptor = characteristic.getDescriptor(BLEConfig.CLIENT_CONFIG_UUID)
            if (descriptor != null) {
                Log.d("BLE", "Found CCCD descriptor for characteristic ${characteristic.uuid}")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val writeSuccess = gatt.writeDescriptor(descriptor)
                Log.d(
                    "BLE",
                    "Write descriptor to enable notifications: $writeSuccess for ${gatt.device?.address ?: "Unknown"}"
                )
                if (!writeSuccess) {
                    Log.e("BLE", "Failed to initiate descriptor write for enabling notifications")
                    retryEnableNotifications(gatt, characteristic, attempt = 1)
                }
            } else {
                Log.e("BLE", "CCCD descriptor not found for characteristic ${characteristic.uuid}")
                retryEnableNotifications(gatt, characteristic, attempt = 1)
            }
        } catch (e: Exception) {
            Log.e("BLE", "Error enabling notifications for ${characteristic.uuid}", e)
            retryEnableNotifications(gatt, characteristic, attempt = 1)
        }
    }

    @SuppressLint("MissingPermission")
    private fun retryEnableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        attempt: Int,
        maxAttempts: Int = 3
    ) {
        if (attempt > maxAttempts) {
            Log.e(
                "BLE",
                "Max retry attempts reached for enabling notifications on ${characteristic.uuid}"
            )
            _connectionStatus.value =
                ConnectionStatus.Error("Failed to enable notifications after $maxAttempts attempts")
            return
        }

        Log.d(
            "BLE",
            "Retrying to enable notifications, attempt $attempt of $maxAttempts for ${characteristic.uuid}"
        )
        try {
            val success = gatt.setCharacteristicNotification(characteristic, true)
            Log.d(
                "BLE",
                "Retry set characteristic notification for ${characteristic.uuid}: $success (attempt $attempt)"
            )

            val descriptor = characteristic.getDescriptor(BLEConfig.CLIENT_CONFIG_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val writeSuccess = gatt.writeDescriptor(descriptor)
                Log.d(
                    "BLE",
                    "Retry write descriptor to enable notifications: $writeSuccess (attempt $attempt) for ${gatt.device?.address ?: "Unknown"}"
                )
            } else {
                Log.e(
                    "BLE",
                    "Descriptor still not found on retry attempt $attempt for ${characteristic.uuid}"
                )
                retryEnableNotifications(gatt, characteristic, attempt + 1, maxAttempts)
            }
        } catch (e: Exception) {
            Log.e("BLE", "Error in retry attempt $attempt for enabling notifications", e)
            retryEnableNotifications(gatt, characteristic, attempt + 1, maxAttempts)
        }
    }


    fun startListening(listener: (List<Float>, String) -> Unit) {
        Log.d("BLE", "Setting new message listener")
        messageListener = listener
    }

    // Clear listener when no longer needed (optional, for cleanup)
    fun clearListener() {
        Log.d("BLE", "Clearing message listener")
        messageListener = null
    }

    // Added: Method to start listening for voice messages
    fun startVoiceMessageListening(listener: (ByteArray, String) -> Unit) {
        Log.d("BLE", "Setting new voice message listener")
        voiceMessageListener = listener
    }

    // Get paired devices
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceData> {
        return bluetoothAdapter?.bondedDevices?.map { device ->
            BluetoothDeviceData(
                name = device.name,
                address = device.address,
                isConnected = isDeviceConnected(device)
            )
        } ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    fun getConnectedDevice(): BluetoothDeviceData? {
        if (!_isConnected.value) return null

        // Check client connection
        gattClient?.device?.let { device ->
            return BluetoothDeviceData(
                name = device.name,
                address = device.address,
                isConnected = true
            )
        }

        // Check server connections using BluetoothManager
        val connectedDevices =
            bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT_SERVER) ?: emptyList()
        connectedDevices.firstOrNull()?.let { device ->
            return BluetoothDeviceData(
                name = device.name,
                address = device.address,
                isConnected = true
            )
        }

        return null
    }

    @SuppressLint("MissingPermission")
    fun stopLeScan() {
        if (!isScanning || leScanCallback == null) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
            isScanning = false
            leScanCallback = null
            Log.d("BLE", "Stopped LE scan")
        } catch (e: Exception) {
            Log.e("BLE", "Error stopping LE scan", e)
            isScanning = false
            leScanCallback = null
        }
    }

    // Disconnect
    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            stopScanning()
            stopLeScan() // Add this to ensure all scans are stopped
        } catch (e: Exception) {
            Log.e("BLE", "Error stopping scanning", e)
        }

        try {
            stopAdvertising()
        } catch (e: Exception) {
            Log.e("BLE", "Error stopping advertising", e)
        }

        try {
            gattClient?.disconnect()
            gattClient?.close()
            gattClient = null
        } catch (e: Exception) {
            Log.e("BLE", "Error closing GATT client", e)
        }

        try {
            gattServer?.close()
            gattServer = null
        } catch (e: Exception) {
            Log.e("BLE", "Error closing GATT server", e)
        }

        _isConnected.value = false
        _connectionStatus.value = ConnectionStatus.Disconnected

        // Added: Reset debouncing variables on disconnect
        lastMessageReceivedTime = 0L
        lastMessageReceived = null

    }

    /**
     * Opens the system Bluetooth settings screen
     */
    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }


    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean {
        val enabled = bluetoothAdapter?.isEnabled == true
        Log.d("BLE", "Checking Bluetooth enabled: $enabled")
        return enabled
    }

    fun enableBluetooth() {
        if (!isBluetoothEnabled()) {
            Log.d("BLE", "Bluetooth is disabled, opening settings")
            openBluetoothSettings()
        } else {
            Log.d("BLE", "Bluetooth is already enabled")
        }
    }

    fun cleanup() {
        // Only unregister if the receiver is actually registered
        if (isReceiverRegistered) {
            try {
                context.applicationContext.unregisterReceiver(bluetoothStateReceiver)
                isReceiverRegistered = false
                Log.d("BLE", "Bluetooth state receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w("BLE", "Receiver was not registered, skipping unregistration", e)
            } catch (e: Exception) {
                Log.e("BLE", "Error unregistering receiver", e)
            }
        } else {
            Log.d("BLE", "Receiver already unregistered, skipping")
        }

        disconnect()
    }

    @SuppressLint("MissingPermission")
    suspend fun sendVoiceMessage(audioBytes: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isConnected.value) {
                    Log.e("BLE", "Cannot send voice message: Not connected")
                    return@withContext false
                }

                // Chunk the audio data if necessary (Bluetooth MTU is typically 20-512 bytes)
                val mtu = 512 // Adjust based on your device's MTU
                val chunkSize = mtu - 3 // Leave room for GATT overhead
                var offset = 0

                while (offset < audioBytes.size) {
                    val chunkLength = minOf(chunkSize, audioBytes.size - offset)
                    val chunk = audioBytes.copyOfRange(offset, offset + chunkLength)
                    val success = sendAsClient(chunk)
                    if (!success) {
                        Log.e("BLE", "Failed to send voice message chunk at offset $offset")
                        return@withContext false
                    }
                    offset += chunkLength
                    delay(50) // Small delay to avoid overwhelming the Bluetooth stack
                }

                Log.d("BLE", "Voice message sent successfully, total bytes: ${audioBytes.size}")
                return@withContext true
            } catch (e: Exception) {
                Log.e("BLE", "Send voice message error", e)
                return@withContext false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendAsClient(data: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val characteristic = writeCharacteristic
                if (characteristic == null) {
                    Log.e("BLE", "Write characteristic not found")
                    return@withContext false
                }

                characteristic.value = data
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                val success = gattClient?.writeCharacteristic(characteristic) ?: false
                if (!success) {
                    Log.e("BLE", "Failed to write characteristic")
                    return@withContext false
                }

                // Wait for the write to complete (BluetoothGattCallback.onCharacteristicWrite)
                delay(100) // Adjust delay as needed
                true
            } catch (e: Exception) {
                Log.e("BLE", "Error sending data as client", e)
                false
            }
        }
    }

    private val messageIdMap =
        mutableMapOf<Pair<String, Int>, Int>() // (deviceAddress, messageId) -> index in messages

    @SuppressLint("MissingPermission")
    fun addMessage(deviceAddress: String, message: Message, messageId: Int? = null) {
        val messages = _messagesPerDevice.getOrPut(deviceAddress) { mutableListOf() }

        // Add the message to the list
        val index = messages.size
        messages.add(message)
        Log.d("BLE", "Added message for device $deviceAddress: $message, ID: $messageId")

        // If a messageId is provided, store it in the map
        if (messageId != null) {
            messageIdMap[Pair(deviceAddress, messageId)] = index
        }

        // Update the messagesFlow to trigger UI updates
        _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }
    }

    @SuppressLint("MissingPermission")
    fun updateMessageStatus(
        deviceAddress: String,
        messageText: String,
        messageId: Int,
        isSentSuccessfully: Boolean
    ) {
        val messages = _messagesPerDevice[deviceAddress]
        if (messages != null) {
            val key = Pair(deviceAddress, messageId)
            val index = messageIdMap[key]
            if (index != null && index < messages.size) {
                val existingMessage = messages[index]
                if (existingMessage is Message.Text && existingMessage.text == messageText && existingMessage.isSentByUser) {
                    messages[index] = existingMessage.copy(isSentSuccessfully = isSentSuccessfully)
                    Log.d(
                        "BLE",
                        "Updated message status for device $deviceAddress: $messageText, ID: $messageId, isSentSuccessfully: $isSentSuccessfully"
                    )
                    messageIdMap.remove(key) // Clean up the map entry
                    _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }
                } else {
                    Log.w("BLE", "Message mismatch for status update: $messageText, ID: $messageId")
                }
            } else {
                Log.w("BLE", "Message not found for status update: $messageText, ID: $messageId")
            }
        }
    }

    // Modified: Overloaded sendAsClient to accept a specific characteristic for voice messages
    @SuppressLint("MissingPermission")
    private suspend fun sendAsClient(
        data: ByteArray,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                characteristic.value = data
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                val success = gattClient?.writeCharacteristic(characteristic) ?: false
                if (!success) {
                    Log.e("BLE", "Failed to write characteristic")
                    return@withContext false
                }

                // Wait for the write to complete (BluetoothGattCallback.onCharacteristicWrite)
                delay(100) // Adjust delay as needed
                true
            } catch (e: Exception) {
                Log.e("BLE", "Error sending data as client", e)
                false
            }
        }
    }

    sealed class ConnectionStatus {
        object Disconnected : ConnectionStatus()
        object Connecting : ConnectionStatus()
        object Connected : ConnectionStatus()
        data class Error(val message: String) : ConnectionStatus()
    }
}
