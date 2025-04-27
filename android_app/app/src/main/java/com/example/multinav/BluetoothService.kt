
package com.example.multinav

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class BluetoothService(private val context: Context) {
    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var gattServer: BluetoothGattServer? = null
    private var gattClient: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false
    private var isAdvertising = false

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    // Add Bluetooth state flow
    private val _bluetoothState = MutableStateFlow(isBluetoothEnabled())
    val bluetoothState: StateFlow<Boolean> = _bluetoothState

    private var messageListener: ((String) -> Unit)? = null
    private val scanResults = mutableMapOf<String, BluetoothDevice>()

    private var lastConnectionStateChange = 0L
    private val connectionStateDebounceMs = 1000L // 1 second debounce

    // Added: Variables for debouncing notifications
    private var lastMessageReceivedTime = 0L
    private var lastMessageReceived: String? = null
    private val messageDebounceMs = 500L // 500ms debounce for notifications

    private var isMobileDevice=false

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
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
                _connectionStatus.value = ConnectionStatus.Error("Failed to start advertising: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("BLE", "Exception in startAdvertising", e)
            _connectionStatus.value = ConnectionStatus.Error("Error in advertising setup: ${e.message}")
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
                _connectionStatus.value = ConnectionStatus.Error("Error after advertising started: ${e.message}")
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
                _connectionStatus.value = ConnectionStatus.Error("Error handling advertising failure")
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
            writeCharacteristic = mobileService.getCharacteristic(BLEConfig.WRITE_CHARACTERISTIC_UUID)
            notifyCharacteristic = mobileService.getCharacteristic(BLEConfig.NOTIFY_CHARACTERISTIC_UUID)
            if (writeCharacteristic == null || notifyCharacteristic == null) {
                Log.e("BLE", "Failed to initialize mobile characteristics")
                _connectionStatus.value = ConnectionStatus.Error("Failed to initialize mobile characteristics")
                gattServer?.close()
                gattServer = null
                return
            }
            val mobileSuccess = gattServer?.addService(mobileService) ?: false

            // Add BLE service
            val bleService = BLEConfig.createBLEChatService()
            val bleWriteCharacteristic = bleService.getCharacteristic(BLEConfig.BLE_WRITE_CHARACTERISTIC_UUID)
            val bleNotifyCharacteristic = bleService.getCharacteristic(BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID)
            if (bleWriteCharacteristic == null || bleNotifyCharacteristic == null) {
                Log.e("BLE", "Failed to initialize BLE characteristics")
                _connectionStatus.value = ConnectionStatus.Error("Failed to initialize BLE characteristics")
                gattServer?.close()
                gattServer = null
                return
            }
            val bleSuccess = gattServer?.addService(bleService) ?: false

            if (mobileSuccess && bleSuccess) {
                Log.d("BLE", "GATT Server started successfully with mobile and BLE services")
                Log.d("BLE", "Mobile Write: ${writeCharacteristic?.uuid}, Notify: ${notifyCharacteristic?.uuid}")
                Log.d("BLE", "BLE Write: ${bleWriteCharacteristic?.uuid}, Notify: ${bleNotifyCharacteristic?.uuid}")
            } else {
                Log.e("BLE", "Failed to add services to GATT Server")
                _connectionStatus.value = ConnectionStatus.Error("Failed to add services to GATT Server")
                gattServer?.close()
                gattServer = null
            }
        } catch (e: Exception) {
            Log.e("BLE", "Exception in startGattServer", e)
            _connectionStatus.value = ConnectionStatus.Error("Error starting GATT server: ${e.message}")
            try {
                gattServer?.close()
                gattServer = null
            } catch (e2: Exception) {
                Log.e("BLE", "Exception cleaning up GATT server", e2)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(address: String, isMobileDevice: Boolean, attempt: Int = 1, maxAttempts: Int = 3): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (attempt > maxAttempts) {
                    _connectionStatus.value = ConnectionStatus.Error("Max connection attempts reached")
                    Log.e("BLE", "Max connection attempts reached for $address")
                    return@withContext false
                }
                stopScanning()
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
                Log.d("BLE", "Initiating connection to $address (attempt $attempt, isMobileDevice: $isMobileDevice)")
                gattClient = device.connectGatt(context, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE)
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
                    Log.e("BLE", "Connection timeout for $address")
                    delay(2000L * attempt)
                    return@withContext connectToDevice(address, isMobileDevice, attempt + 1, maxAttempts)
                }
                Log.d("BLE", "Connection successful to $address")
                saveLastConnectedDevice(address, isMobileDevice)
                return@withContext true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("BLE", "Connection error on attempt $attempt", e)
                _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Unknown error")
                delay(2000L * attempt)
                return@withContext connectToDevice(address, isMobileDevice, attempt + 1, maxAttempts)
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
                if (!_isConnected.value) {
                    Log.e("BLE", "Cannot send message: Not connected")
                    return@withContext false
                }
                if (gattServer != null) {
                    val success = sendAsServer(message, isMobileDevice)
                    if (success) {
                        Log.d("BLE", "Message sent as server: $message")
                        return@withContext true
                    }
                }
                if (gattClient != null) {
                    // Pass isMobileDevice to sendAsClient
                    val success = sendAsClient(message, isMobileDevice)
                    if (success) {
                        Log.d("BLE", "Message sent as client: $message")
                        return@withContext true
                    }
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
                Log.d("BLE", "Found device: ${device.name} (${device.address}), RSSI: $rssi, Service UUIDs: $serviceUuids")
                // Still classify based on expected UUIDs for UI purposes
                val isMobileDevice = result.scanRecord?.serviceUuids?.contains(ParcelUuid(BLEConfig.CHAT_SERVICE_UUID)) == true
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
            gattServer?.getService(BLEConfig.CHAT_BLE_SERVICE_UUID)?.getCharacteristic(BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID)
        }

        notifyChar?.let { characteristic ->
            try {
                characteristic.value = message.toByteArray()
                val connectedDevices = bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT_SERVER) ?: emptyList()
                if (connectedDevices.isEmpty()) {
                    Log.e("BLE", "No connected devices found for server send operation")
                    return false
                }
                Log.d("BLE", "Found ${connectedDevices.size} connected device(s) for server send")

                var success = false
                for (device in connectedDevices) {
                    val result = gattServer?.notifyCharacteristicChanged(device, characteristic, false) ?: false
                    success = success || result
                    Log.d("BLE", "Server sent message: $message to ${device.address}, success: $result")
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
    private fun sendAsClient(message: String, isMobileDevice: Boolean): Boolean {
        val serviceUuid = if (isMobileDevice) BLEConfig.CHAT_SERVICE_UUID else BLEConfig.CHAT_BLE_SERVICE_UUID
        val writeUuid = if (isMobileDevice) BLEConfig.WRITE_CHARACTERISTIC_UUID else BLEConfig.BLE_WRITE_CHARACTERISTIC_UUID

        val service = gattClient?.getService(serviceUuid)
        if (service == null) {
            Log.e("BLE", "Chat service not found for client send (UUID: $serviceUuid)")
            return false
        }

        val characteristic = service.getCharacteristic(writeUuid)
        if (characteristic == null) {
            Log.e("BLE", "Write characteristic not found for client send (UUID: $writeUuid)")
            return false
        }

        try {
            characteristic.writeType = if (isMobileDevice) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            characteristic.value = message.toByteArray()
            val success = gattClient?.writeCharacteristic(characteristic) ?: false
            if (success) {
                Log.d("BLE", "Client sent message: $message to $serviceUuid, success: $success")
            } else {
                Log.e("BLE", "Failed to send message: $message to $serviceUuid")
            }
            return success
        } catch (e: Exception) {
            Log.e("BLE", "Error sending message as client: $message", e)
            return false
        }
    }

    @SuppressLint("MissingPermission")
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        // Check client connection
        if (_isConnected.value && gattClient?.device?.address == device.address) {
            return true
        }

        // Check server connections using BluetoothManager instead of GattServer
        val connectedDevices = bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT_SERVER) ?: emptyList()
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
            if (characteristic.uuid == BLEConfig.WRITE_CHARACTERISTIC_UUID) {
                val message = String(value)
                Log.d("BLE", "Server received message: $message from ${device.address}")
                messageListener?.let { listener ->
                    Log.d("BLE", "Invoking listener with message: $message")
                    listener(message)
                } ?: Log.w("BLE", "No listener set for received message: $message")

                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                    Log.d("BLE", "Sent response to write request from ${device.address}")

                }
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

                Log.d("BLE", "Client connection state change: $status -> $newState for ${gatt.device?.address ?: "Unknown"}")
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _isConnected.value = true
                        _connectionStatus.value = ConnectionStatus.Connected

                        // Add a small delay before discovering services
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                if (_isConnected.value && gatt.device != null) {
                                    Log.d("BLE", "Starting service discovery for ${gatt.device.address}")
                                    val success = gatt.discoverServices()
                                    Log.d("BLE", "Service discovery initiated: $success")
                                }
                            } catch (e: Exception) {
                                Log.e("BLE", "Error discovering services", e)
                            }
                        }, 500)

                        Log.d("BLE", "Connected to server: ${gatt.device.address}")
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _isConnected.value = false
                        _connectionStatus.value = ConnectionStatus.Disconnected
                        Log.d("BLE", "Disconnected from server: ${gatt.device?.address ?: "Unknown"}")
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

                    val serviceUuid = if (isMobileDevice) BLEConfig.CHAT_SERVICE_UUID else BLEConfig.CHAT_BLE_SERVICE_UUID
                    val notifyUuid = if (isMobileDevice) BLEConfig.NOTIFY_CHARACTERISTIC_UUID else BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID
                    val service = gatt.getService(serviceUuid)

                    if (service != null) {
                        writeCharacteristic = service.getCharacteristic(
                            if (isMobileDevice) BLEConfig.WRITE_CHARACTERISTIC_UUID else BLEConfig.BLE_WRITE_CHARACTERISTIC_UUID
                        )
                        notifyCharacteristic = service.getCharacteristic(notifyUuid)

                        Log.d("BLE", "Chat service found (UUID: $serviceUuid). Write: ${writeCharacteristic?.uuid}, Notify: ${notifyCharacteristic?.uuid}")

                        if (notifyCharacteristic != null) {
                            // Enable notifications immediately
                            val success = gatt.setCharacteristicNotification(notifyCharacteristic, true)
                            Log.d("BLE", "Set characteristic notification: $success")
                            if (success) {
                                enableNotifications(gatt, notifyCharacteristic)
                            } else {
                                Log.e("BLE", "Failed to enable notifications at GATT level")
                                retryEnableNotifications(gatt, notifyCharacteristic!!, attempt = 1)
                            }
                        } else {
                            Log.e("BLE", "Notify characteristic not found (UUID: $notifyUuid)")
                            _connectionStatus.value = ConnectionStatus.Error("Notify characteristic not found")
                        }
                    } else {
                        Log.e("BLE", "Chat service not found (UUID: $serviceUuid)")
                        _connectionStatus.value = ConnectionStatus.Error("Chat service not found")
                    }
                } else {
                    Log.e("BLE", "Service discovery failed with status: $status")
                    _connectionStatus.value = ConnectionStatus.Error("Service discovery failed: $status")
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error in onServicesDiscovered", e)
                _connectionStatus.value = ConnectionStatus.Error("Error discovering services: ${e.message}")
            }
        }

        // For Android 13+ (API 33+)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            try {
                handleCharacteristicChanged(characteristic, value)
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
                handleCharacteristicChanged(characteristic, characteristic.value)
            } catch (e: Exception) {
                Log.e("BLE", "Error in deprecated onCharacteristicChanged", e)
            }
        }

        private fun handleCharacteristicChanged(
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            try {
                val message = String(value)
                val currentTime = System.currentTimeMillis()

                // Debounce messages
                if (message == lastMessageReceived && (currentTime - lastMessageReceivedTime) < messageDebounceMs) {
                    Log.d("BLE", "Ignoring duplicate message within debounce period: $message")
                    return
                }

                lastMessageReceived = message
                lastMessageReceivedTime = currentTime

                Log.d("BLE", "Client received message: $message from characteristic: ${characteristic.uuid}")
                val expectedUuid = if (characteristic.service.uuid == BLEConfig.CHAT_SERVICE_UUID) {
                    BLEConfig.NOTIFY_CHARACTERISTIC_UUID
                } else {
                    BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID
                }
                if (characteristic.uuid == expectedUuid) {
                    messageListener?.invoke(message) ?: Log.w("BLE", "No listener set for received message: $message on client")
                } else {
                    Log.w("BLE", "Received message on unexpected characteristic: ${characteristic.uuid}")
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
                Log.d("BLE", "Descriptor write completed with status: $status for ${descriptor.uuid}")
                if (descriptor.uuid == BLEConfig.CLIENT_CONFIG_UUID) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d("BLE", "Notifications successfully enabled")
                    } else {
                        Log.e("BLE", "Failed to enable notifications, status: $status")
                        val localNotifyCharacteristic = notifyCharacteristic
                        if (localNotifyCharacteristic != null) {
                            retryEnableNotifications(gatt, localNotifyCharacteristic, attempt = 1)
                        } else {
                            Log.e("BLE", "Cannot retry enabling notifications: notifyCharacteristic is null")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error in onDescriptorWrite", e)
            }
        }

    }


    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?) {
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
                Log.d("BLE", "Write descriptor to enable notifications: $writeSuccess for ${gatt.device?.address ?: "Unknown"}")
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
    private fun retryEnableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, attempt: Int, maxAttempts: Int = 3) {
        if (attempt > maxAttempts) {
            Log.e("BLE", "Max retry attempts reached for enabling notifications on ${characteristic.uuid}")
            _connectionStatus.value = ConnectionStatus.Error("Failed to enable notifications after $maxAttempts attempts")
            return
        }

        Log.d("BLE", "Retrying to enable notifications, attempt $attempt of $maxAttempts for ${characteristic.uuid}")
        try {
            val success = gatt.setCharacteristicNotification(characteristic, true)
            Log.d("BLE", "Retry set characteristic notification for ${characteristic.uuid}: $success (attempt $attempt)")

            val descriptor = characteristic.getDescriptor(BLEConfig.CLIENT_CONFIG_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val writeSuccess = gatt.writeDescriptor(descriptor)
                Log.d("BLE", "Retry write descriptor to enable notifications: $writeSuccess (attempt $attempt) for ${gatt.device?.address ?: "Unknown"}")
            } else {
                Log.e("BLE", "Descriptor still not found on retry attempt $attempt for ${characteristic.uuid}")
                retryEnableNotifications(gatt, characteristic, attempt + 1, maxAttempts)
            }
        } catch (e: Exception) {
            Log.e("BLE", "Error in retry attempt $attempt for enabling notifications", e)
            retryEnableNotifications(gatt, characteristic, attempt + 1, maxAttempts)
        }
    }


    // Start listening for messages, ensuring the latest listener is used
    fun startListening(listener: (String) -> Unit) {
        Log.d("BLE", "Setting new message listener")
        messageListener = listener
    }
    // Clear listener when no longer needed (optional, for cleanup)
    fun clearListener() {
        Log.d("BLE", "Clearing message listener")
        messageListener = null
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
        val connectedDevices = bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT_SERVER) ?: emptyList()
        connectedDevices.firstOrNull()?.let { device ->
            return BluetoothDeviceData(
                name = device.name,
                address = device.address,
                isConnected = true
            )
        }

        return null
    }

    // Disconnect
    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            stopScanning()
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
        context.unregisterReceiver(bluetoothStateReceiver)
        disconnect()
    }


    sealed class ConnectionStatus {
        object Disconnected : ConnectionStatus()
        object Connecting : ConnectionStatus()
        object Connected : ConnectionStatus()
        data class Error(val message: String) : ConnectionStatus()
    }
}