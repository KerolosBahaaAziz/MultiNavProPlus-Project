package com.example.multinav

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
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

    private var messageListener: ((String) -> Unit)? = null
    private val scanResults = mutableMapOf<String, BluetoothDevice>()

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
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
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
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
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
            // Close any existing GATT server
            gattServer?.close()
            gattServer = null

            // Open a new GATT server
            gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)

            if (gattServer == null) {
                Log.e("BLE", "Failed to open GATT server")
                _connectionStatus.value = ConnectionStatus.Error("Failed to open GATT server")
                return
            }

            // Create the service
            val service = BLEConfig.createChatService()

            // Store characteristics before adding service
            writeCharacteristic = service.getCharacteristic(BLEConfig.WRITE_CHARACTERISTIC_UUID)
            notifyCharacteristic = service.getCharacteristic(BLEConfig.NOTIFY_CHARACTERISTIC_UUID)

            // Add the service
            val success = gattServer?.addService(service) ?: false

            if (success) {
                Log.d("BLE", "GATT Server started successfully")
                Log.d("BLE", "Write characteristic: ${writeCharacteristic?.uuid}")
                Log.d("BLE", "Notify characteristic: ${notifyCharacteristic?.uuid}")
            } else {
                Log.e("BLE", "Failed to add service to GATT Server")
                _connectionStatus.value = ConnectionStatus.Error("Failed to add service to GATT Server")

                // Clean up
                gattServer?.close()
                gattServer = null
            }
        } catch (e: Exception) {
            Log.e("BLE", "Exception in startGattServer", e)
            _connectionStatus.value = ConnectionStatus.Error("Error starting GATT server: ${e.message}")

            // Clean up
            try {
                gattServer?.close()
                gattServer = null
            } catch (e2: Exception) {
                Log.e("BLE", "Exception cleaning up GATT server", e2)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(address: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                stopScanning()
                stopAdvertising()
                _connectionStatus.value = ConnectionStatus.Connecting
                val device = bluetoothAdapter?.getRemoteDevice(address)
                if (device == null) {
                    _connectionStatus.value = ConnectionStatus.Error("Device not found")
                    return@withContext false
                }

                // Close any existing connection
                try {
                    gattClient?.close()
                    gattClient = null
                } catch (e: Exception) {
                    Log.e("BLE", "Error closing existing GATT client", e)
                    // Continue anyway
                }

                // Connect with autoConnect=false for immediate connection
                gattClient = device.connectGatt(context, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE)

                if (gattClient == null) {
                    _connectionStatus.value = ConnectionStatus.Error("Failed to create GATT client")
                    return@withContext false
                }

                // Wait for connection to establish (max 5 seconds)
                var timeout = 0
                while (!_isConnected.value && timeout < 50) {
                    delay(100)
                    timeout++

                    // Check if the coroutine was cancelled
                    ensureActive()
                }

                if (!_isConnected.value) {
                    _connectionStatus.value = ConnectionStatus.Error("Connection timeout")
                    return@withContext false
                }

                return@withContext true
            } catch (e: CancellationException) {
                // Rethrow cancellation exceptions to properly cancel the coroutine
                throw e
            } catch (e: Exception) {
                Log.e("BLE", "Connection error", e)
                _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Unknown error")
                false
            }
        }
    }

    // Send a message
    @SuppressLint("MissingPermission")
    suspend fun sendMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isConnected.value) {
                    Log.e("BLE", "Cannot send message: Not connected")
                    return@withContext false
                }

                // Try to send as server first
                if (gattServer != null) {
                    val success = sendAsServer(message)
                    if (success) {
                        Log.d("BLE", "Message sent as server: $message")
                        return@withContext true
                    }
                }

                // If server send failed or we're not a server, try as client
                if (gattClient != null) {
                    val success = sendAsClient(message)
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
    private fun sendAsServer(message: String): Boolean {
        notifyCharacteristic?.let { characteristic ->
            characteristic.value = message.toByteArray()
            var success = false

            // Notify all connected clients
            gattServer?.connectedDevices?.forEach { device ->
                success = gattServer?.notifyCharacteristicChanged(device, characteristic, false) ?: false
                Log.d("BLE", "Server sent message: $message to ${device.address}, success: $success")
            }

            return success
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun sendAsClient(message: String): Boolean {
        val service = gattClient?.getService(BLEConfig.CHAT_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(BLEConfig.WRITE_CHARACTERISTIC_UUID)

        if (characteristic != null) {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.value = message.toByteArray()
            val success = gattClient?.writeCharacteristic(characteristic) ?: false
            Log.d("BLE", "Client sent message: $message, success: $success")
            return success
        } else {
            Log.e("BLE", "Write characteristic not found for client")
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
                messageListener?.invoke(message)

                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
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
                Log.d("BLE", "Client connection state change: $status -> $newState")
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _isConnected.value = true
                        _connectionStatus.value = ConnectionStatus.Connected

                        // Add a small delay before discovering services
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                if (_isConnected.value && gatt.device != null) {
                                    Log.d("BLE", "Starting service discovery")
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
                    // Log all available services for debugging
                    Log.d("BLE", "Services discovered:")
                    try {
                        gatt.services.forEach { service ->
                            Log.d("BLE", "Service: ${service.uuid}")
                            service.characteristics.forEach { characteristic ->
                                Log.d("BLE", "  Characteristic: ${characteristic.uuid}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BLE", "Error logging services", e)
                    }

                    try {
                        val service = gatt.getService(BLEConfig.CHAT_SERVICE_UUID)
                        if (service != null) {
                            writeCharacteristic = service.getCharacteristic(BLEConfig.WRITE_CHARACTERISTIC_UUID)
                            notifyCharacteristic = service.getCharacteristic(BLEConfig.NOTIFY_CHARACTERISTIC_UUID)

                            Log.d("BLE", "Chat service found. Write: ${writeCharacteristic?.uuid}, Notify: ${notifyCharacteristic?.uuid}")

                            // Enable notifications
                            if (notifyCharacteristic != null) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    try {
                                        enableNotifications(gatt, notifyCharacteristic)
                                    } catch (e: Exception) {
                                        Log.e("BLE", "Error enabling notifications", e)
                                    }
                                }, 300)
                            } else {
                                Log.e("BLE", "Notify characteristic not found")
                            }
                        } else {
                            Log.e("BLE", "Chat service not found")

                            // Try to discover services again after a delay
                            Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    if (_isConnected.value && gatt.device != null) {
                                        Log.d("BLE", "Retrying service discovery")
                                        gatt.discoverServices()
                                    }
                                } catch (e: Exception) {
                                    Log.e("BLE", "Error retrying service discovery", e)
                                }
                            }, 1000)
                        }
                    } catch (e: Exception) {
                        Log.e("BLE", "Error processing discovered services", e)
                    }
                } else {
                    Log.e("BLE", "Service discovery failed with status: $status")

                    // Try to discover services again after a delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            if (_isConnected.value && gatt.device != null) {
                                Log.d("BLE", "Retrying service discovery after failure")
                                gatt.discoverServices()
                            }
                        } catch (e: Exception) {
                            Log.e("BLE", "Error retrying service discovery after failure", e)
                        }
                    }, 1000)
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error in onServicesDiscovered", e)
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
                Log.d("BLE", "Received message: $message from characteristic: ${characteristic.uuid}")

                if (characteristic.uuid == BLEConfig.NOTIFY_CHARACTERISTIC_UUID) {
                    messageListener?.invoke(message)
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
                if (descriptor.uuid == BLEConfig.CLIENT_CONFIG_UUID && status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLE", "Notifications successfully enabled")
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error in onDescriptorWrite", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?) {
        characteristic?.let {
            try {
                // First enable notifications at the GATT level
                val success = gatt.setCharacteristicNotification(it, true)
                Log.d("BLE", "Set characteristic notification: $success")

                // Add a delay before writing to the descriptor
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        // Then write to the CCCD
                        val descriptor = it.getDescriptor(BLEConfig.CLIENT_CONFIG_UUID)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            val writeSuccess = gatt.writeDescriptor(descriptor)
                            Log.d("BLE", "Write descriptor: $writeSuccess")
                        } else {
                            Log.e("BLE", "Descriptor not found for enabling notifications")
                        }
                    } catch (e: Exception) {
                        Log.e("BLE", "Error in delayed descriptor write", e)
                    }
                }, 300)
            } catch (e: Exception) {
                Log.e("BLE", "Error enabling notifications", e)
            }
        }
    }

    // Start listening for messages
    fun startListening(listener: (String) -> Unit) {
        messageListener = { message ->
            Log.d("BLE", "Received message: $message")
            listener(message)
        }
        Log.d("BLE", "Started listening for messages")
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
    }

    /**
     * Opens the system Bluetooth settings screen
     */
    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }



    sealed class ConnectionStatus {
        object Disconnected : ConnectionStatus()
        object Connecting : ConnectionStatus()
        object Connected : ConnectionStatus()
        data class Error(val message: String) : ConnectionStatus()
    }
}