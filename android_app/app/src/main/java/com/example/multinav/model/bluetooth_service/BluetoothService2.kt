package com.example.multinav.model.bluetooth_service

import android.Manifest
import com.example.multinav.chat.Message
import com.example.multinav.utils.ByteUtils


import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BluetoothService(private val context: Context) {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var gattClient: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceData>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceData>> = _scannedDevices

    private val _isInitialScanning = MutableStateFlow(false)
    val isInitialScanningState: StateFlow<Boolean> = _isInitialScanning

    var isScanning = false

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _bluetoothState = MutableStateFlow(isBluetoothEnabled())
    val bluetoothState: StateFlow<Boolean> = _bluetoothState

    private val bluetoothLeScanner by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    // Messages storage
    private val _messagesPerDevice = ConcurrentHashMap<String, MutableList<Message>>()
    private val _messagesFlow = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val messagesFlow: StateFlow<Map<String, List<Message>>> = _messagesFlow.asStateFlow()

    // Listeners
    private var messageListener: ((List<Float>, String) -> Unit)? = null
    private var voiceMessageListener: ((ByteArray, String) -> Unit)? = null

    // Connection management
    private var lastConnectionStateChange = 0L
    private val connectionStateDebounceMs = 1000L
    private var lastConnectedDeviceAddress: String? = null

    private val _receivedAudioData = MutableStateFlow<ByteArray>(ByteArray(0))
    val receivedAudioData: StateFlow<ByteArray> = _receivedAudioData.asStateFlow()

    // Protocol for RFOXIA CHAT
    private object MessageProtocol {
        const val TYPE_TEXT = 0x00.toByte()
        const val TYPE_VOICE_START = 0x01.toByte()
        const val TYPE_VOICE_DATA = 0x02.toByte()
        const val TYPE_VOICE_END = 0x03.toByte()
    }

    // Audio buffer for receiving voice messages
    private val audioBuffer = mutableListOf<ByteArray>()

    // BLE module scan related
    private val deviceListBuffer = StringBuilder()
    private var scanTimeoutJob: Job? = null
    private val _scannedDevicesFromBle = MutableStateFlow<List<BluetoothDeviceData>>(emptyList())
    val scannedDevicesFromBle: StateFlow<List<BluetoothDeviceData>> = _scannedDevicesFromBle
    private val _isScanning = MutableStateFlow(false)
    val isScanningState: StateFlow<Boolean> = _isScanning

    // Sensor characteristics management
    private var pendingSensorIndex = 0
    private val sensorCharacteristicsList = listOf(
        BLEConfig.TEMPERATURE_CHARACTERISTIC_UUID,
        BLEConfig.HUMIDITY_CHARACTERISTIC_UUID,
        BLEConfig.AIR_PRESSURE_CHARACTERISTIC_UUID,
        BLEConfig.AIR_QUALITY_CHARACTERISTIC_UUID
    )
    private var pendingUnsubscribeIndex = 0

    val connectedDeviceName: String?
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        get() = gattClient?.device?.name

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
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)
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

    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun enableBluetooth() {
        if (!isBluetoothEnabled()) {
            Log.d("BLE", "Bluetooth is disabled, opening settings")
            openBluetoothSettings()
        }
    }

    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    suspend fun startInitialBleScan(): Boolean {
        if (!isBluetoothEnabled()) {
            Log.e("BLE", "Bluetooth is disabled")
            enableBluetooth()
            return false
        }
        if (!isLocationEnabled()) {
            Log.e("BLE", "Location services disabled")
            enableLocation()
            return false
        }

        _scannedDevices.value = emptyList()
        _isInitialScanning.value = true

        return withContext(Dispatchers.IO) {
            try {
                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                val scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val device = result.device
                        val deviceData = BluetoothDeviceData(
                            name = device.name ?: "Unknown",
                            address = device.address,
                            rssi = result.rssi,
                            isConnected = false,
                            isMobileDevice = false,
                            clickCount = 0
                        )
                        _scannedDevices.update { current ->
                            if (current.none { it.address == deviceData.address }) {
                                current + deviceData
                            } else {
                                current
                            }
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e("BLE", "Initial scan failed with error: $errorCode")
                        _isInitialScanning.value = false
                    }
                }

                bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)
                delay(10000) // Scan for 10 seconds
                bluetoothLeScanner?.stopScan(scanCallback)
                _isInitialScanning.value = false
                true
            } catch (e: SecurityException) {
                Log.e("BLE", "Permission error during initial scan", e)
                _isInitialScanning.value = false
                false
            } catch (e: Exception) {
                Log.e("BLE", "Initial scan error", e)
                _isInitialScanning.value = false
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(
        address: String,
        attempt: Int = 1,
        maxAttempts: Int = 3
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (attempt > maxAttempts) {
                    _connectionStatus.value = ConnectionStatus.Error("Max connection attempts reached")
                    return@withContext false
                }

                if (_isConnected.value) {
                    disconnect()
                    delay(500)
                }

                _connectionStatus.value = ConnectionStatus.Connecting
                val device = bluetoothAdapter?.getRemoteDevice(address)
                if (device == null) {
                    _connectionStatus.value = ConnectionStatus.Error("Device not found")
                    Log.e("BLE", "Device not found for address: $address")
                    return@withContext false
                }

                gattClient?.close()
                gattClient = null

                gattClient = device.connectGatt(
                    context,
                    false,
                    gattClientCallback,
                    BluetoothDevice.TRANSPORT_LE
                )

                if (gattClient == null) {
                    _connectionStatus.value = ConnectionStatus.Error("Failed to create GATT client")
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
                    return@withContext connectToDevice(address, attempt + 1, maxAttempts)
                }

                saveLastConnectedAddress(address)

                if (!_messagesPerDevice.containsKey(address)) {
                    _messagesPerDevice[address] = mutableListOf()
                    _messagesFlow.value = _messagesPerDevice.toMap()
                }

                return@withContext true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("BLE", "Connection error on attempt $attempt", e)
                _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Unknown error")
                delay(2000L * attempt)
                return@withContext connectToDevice(address, attempt + 1, maxAttempts)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun sendTextMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isConnected.value) {
                    Log.e("BLE", "Cannot send message: Not connected")
                    return@withContext false
                }

                val characteristic = writeCharacteristic ?: run {
                    Log.e("BLE", "Write characteristic not found")
                    return@withContext false
                }

                // Add protocol header for RFOXIA CHAT
                val messageBytes = message.toByteArray(Charsets.UTF_8)
                if (messageBytes.size > BLEConfig.RFOXIA_CHAT_MAX_BYTES - 1) {
                    Log.e("BLE", "Message too long: ${messageBytes.size + 1} bytes (max: ${BLEConfig.RFOXIA_CHAT_MAX_BYTES})")
                    return@withContext false
                }
                val packet = byteArrayOf(MessageProtocol.TYPE_TEXT) + messageBytes

                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                characteristic.value = packet
                val success = gattClient?.writeCharacteristic(characteristic) ?: false

                if (success) {
                    Log.d("BLE", "Text message sent: $message (${packet.size} bytes)")
                    gattClient?.device?.address?.let { deviceAddress ->
                        val messages = _messagesPerDevice.getOrPut(deviceAddress) { mutableListOf() }
                        messages.add(Message.Text(message, true))
                        _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }
                    }
                }

                return@withContext success
            } catch (e: Exception) {
                Log.e("BLE", "Send text message error", e)
                return@withContext false
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun sendVoiceMessage(audioBytes: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isConnected.value) {
                    Log.e("BLE", "Cannot send voice message: Not connected")
                    return@withContext false
                }

                val characteristic = writeCharacteristic ?: run {
                    Log.e("BLE", "Write characteristic not found")
                    return@withContext false
                }

                val maxDataSize = BLEConfig.RFOXIA_CHAT_MAX_BYTES - 1
                var offset = 0
                var chunkIndex = 0

                // Send start packet
                val startPacket = byteArrayOf(MessageProtocol.TYPE_VOICE_START)
                characteristic.value = startPacket
                var success = gattClient?.writeCharacteristic(characteristic) ?: false
                if (!success) {
                    Log.e("BLE", "Failed to send voice start packet")
                    return@withContext false
                }
                delay(50)

                // Send voice data in chunks
                while (offset < audioBytes.size) {
                    val remainingBytes = audioBytes.size - offset
                    val chunkSize = minOf(maxDataSize, remainingBytes)
                    val chunk = audioBytes.copyOfRange(offset, offset + chunkSize)

                    val packet = byteArrayOf(MessageProtocol.TYPE_VOICE_DATA) + chunk

                    Log.d("BLE", "Sending voice chunk $chunkIndex: ${packet.size} bytes")

                    characteristic.value = packet
                    success = gattClient?.writeCharacteristic(characteristic) ?: false

                    if (!success) {
                        Log.e("BLE", "Failed to send voice chunk at offset $offset")
                        return@withContext false
                    }

                    offset += chunkSize
                    chunkIndex++
                    delay(50)
                }

                // Send end packet
                val endPacket = byteArrayOf(MessageProtocol.TYPE_VOICE_END)
                characteristic.value = endPacket
                success = gattClient?.writeCharacteristic(characteristic) ?: false

                if (success) {
                    Log.d("BLE", "Voice message sent: ${audioBytes.size} bytes in $chunkIndex chunks")
                    gattClient?.device?.address?.let { deviceAddress ->
                        val messages = _messagesPerDevice.getOrPut(deviceAddress) { mutableListOf() }
                        messages.add(Message.Voice(audioBytes, true))
                        _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }
                    }
                }

                return@withContext success
            } catch (e: Exception) {
                Log.e("BLE", "Send voice message error", e)
                return@withContext false
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun requestBleModuleScan(): Boolean {
        if (!isBluetoothEnabled()) {
            Log.e("BLE", "Bluetooth is disabled")
            enableBluetooth()
            return false
        }
        if (!isLocationEnabled()) {
            Log.e("BLE", "Location services disabled")
            enableLocation()
            return false
        }
        if (!_isConnected.value) {
            Log.e("BLE", "Cannot request scan: Not connected to BLE module")
            return false
        }

        _scannedDevicesFromBle.value = emptyList()
        deviceListBuffer.clear()
        _isScanning.value = true

        val service = gattClient?.getService(BLEConfig.BLIST_CONNECTION_SERVICE_UUID)
        if (service == null) {
            Log.e("BLE", "Service not found")
            _isScanning.value = false
            return false
        }

        val bStateChar = service.getCharacteristic(BLEConfig.B_STATE_CHARACTERISTIC_UUID)
        val bListChar = service.getCharacteristic(BLEConfig.B_LIST_CHARACTERISTIC_UUID)

        if (bStateChar == null || bListChar == null) {
            Log.e("BLE", "Required characteristics not found")
            _isScanning.value = false
            return false
        }

        var setNotificationSuccess = false
        repeat(3) { attempt ->
            setNotificationSuccess = gattClient?.setCharacteristicNotification(bListChar, true) ?: false
            if (setNotificationSuccess) {
                val descriptor = bListChar.getDescriptor(BLEConfig.CLIENT_CONFIG_UUID)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    if (gattClient?.writeDescriptor(descriptor) == true) {
                        delay(500)
                        return@repeat
                    }
                }
            }
            Log.w("BLE", "Failed to enable notifications for B_LIST, attempt ${attempt + 1}")
            delay(100)
        }
        if (!setNotificationSuccess) {
            Log.e("BLE", "Failed to enable notifications for B_LIST")
            _isScanning.value = false
            return false
        }

        var writeSuccess = false
        repeat(3) { attempt ->
            bStateChar.value = "c".toByteArray(Charsets.UTF_8)
            writeSuccess = gattClient?.writeCharacteristic(bStateChar) ?: false
            if (writeSuccess) return@repeat
            Log.w("BLE", "Failed to write scan command, attempt ${attempt + 1}")
            delay(100)
        }
        if (!writeSuccess) {
            Log.e("BLE", "Failed to send scan command to BLE module")
            _isScanning.value = false
            return false
        }

        scanTimeoutJob?.cancel()
        scanTimeoutJob = CoroutineScope(Dispatchers.IO).launch {
            delay(30000)
            if (_isScanning.value) {
                Log.d("BLE", "Scan timeout")
                _isScanning.value = false
                if (deviceListBuffer.isNotEmpty()) {
                    processReceivedDeviceList()
                }
            }
        }

        return true
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDeviceByIndex(index: Int): Boolean {
        if (!_isConnected.value) {
            Log.e("BLE", "Cannot connect: Not connected to BLE module")
            return false
        }

        val bleService = gattClient?.getService(BLEConfig.BLIST_CONNECTION_SERVICE_UUID)
        val bStateChar = bleService?.getCharacteristic(BLEConfig.B_STATE_CHARACTERISTIC_UUID)

        if (bStateChar == null) {
            Log.e("BLE", "B_STATE characteristic not found")
            return false
        }

        if (index < 0 || index > 10) {
            Log.e("BLE", "Invalid device index: $index")
            return false
        }

        val byteValue = byteArrayOf(index.toByte())
        bStateChar.value = byteValue
        val writeSuccess = gattClient?.writeCharacteristic(bStateChar) ?: false

        Log.d("BLE", "Sent connect command with index: $index, success: $writeSuccess")
        return writeSuccess
    }

    fun startListening(listener: (List<Float>, String) -> Unit) {
        Log.d("BLE", "Setting new message listener")
        messageListener = listener
    }

    fun getConnectedDeviceAddress(): String? {
        if (!isConnected.value) return null
        return gattClient?.device?.address ?: lastConnectedDeviceAddress
    }

    fun saveLastConnectedAddress(address: String) {
        lastConnectedDeviceAddress = address
        Log.d("BLE", "Saved last connected address: $address")
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            startUnsubscribeProcess()
        } catch (e: Exception) {
            Log.e("BLE", "Error starting unsubscribe process", e)
            completeDisconnect()
        }
    }

    // GATT Client Callback
    private val gattClientCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastConnectionStateChange < connectionStateDebounceMs) {
                    Log.d("BLE", "Ignoring rapid connection state change")
                    return
                }
                lastConnectionStateChange = currentTime

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _isConnected.value = true
                        _connectionStatus.value = ConnectionStatus.Connected

                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                if (_isConnected.value && gatt.device != null) {
                                    gatt.discoverServices()
                                }
                            } catch (e: Exception) {
                                Log.e("BLE", "Error discovering services", e)
                            }
                        }, 500)

                        gatt.device.address?.let { deviceAddress ->
                            if (!_messagesPerDevice.containsKey(deviceAddress)) {
                                _messagesPerDevice[deviceAddress] = mutableListOf()
                                _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }
                            }
                        }
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _isConnected.value = false
                        _connectionStatus.value = ConnectionStatus.Disconnected
                        writeCharacteristic = null
                        notifyCharacteristic = null
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
                    val service = gatt.getService(BLEConfig.BLE_SERVICE_UUID)
                    if (service != null) {
                        writeCharacteristic = service.getCharacteristic(BLEConfig.BLE_WRITE_CHARACTERISTIC_UUID)
                        notifyCharacteristic = service.getCharacteristic(BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID)

                        if (notifyCharacteristic != null) {
                            val success = gatt.setCharacteristicNotification(notifyCharacteristic, true)
                            if (success) {
                                enableNotifications(gatt, notifyCharacteristic)
                            }
                        }
                    } else {
                        Log.e("BLE", "Chat service not found")
                        _connectionStatus.value = ConnectionStatus.Error("Chat service not found")
                    }

                    val sensorService = gatt.getService(BLEConfig.SENSOR_SERVICE_UUID)
                    if (sensorService != null) {
                        subscribeToSensorCharacteristics(gatt)
                    }
                } else {
                    Log.e("BLE", "Service discovery failed with status: $status")
                    _connectionStatus.value = ConnectionStatus.Error("Service discovery failed")
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error in onServicesDiscovered", e)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicChanged(gatt, characteristic, value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            handleCharacteristicChanged(gatt, characteristic, characteristic.value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.d("BLE", "Write completed with status: $status")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            handleDescriptorWrite(gatt, descriptor, status)
        }
    }

    private fun handleCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        try {
            val deviceAddress = gatt.device?.address ?: "Unknown"

            when {
                characteristic.uuid == BLEConfig.B_STATE_CHARACTERISTIC_UUID -> {
                    val stateData = String(value, Charsets.UTF_8)
                    if (stateData.contains("R")) {
                        _isScanning.value = false
                        scanTimeoutJob?.cancel()
                        if (deviceListBuffer.isNotEmpty()) {
                            processReceivedDeviceList()
                        }
                    }
                }

                characteristic.uuid == BLEConfig.B_LIST_CHARACTERISTIC_UUID -> {
                    val stringData = String(value, Charsets.UTF_8)
                    processBleModuleScanResult(stringData)
                }

                characteristic.service.uuid == BLEConfig.SENSOR_SERVICE_UUID -> {
                    processSensorData(characteristic, value, deviceAddress)
                }

                characteristic.uuid == BLEConfig.BLE_NOTIFY_CHARACTERISTIC_UUID -> {
                    handleRfoxiaChatMessage(value, deviceAddress)
                }
            }
        } catch (e: Exception) {
            Log.e("BLE", "Error handling characteristic change", e)
        }
    }

    private fun handleRfoxiaChatMessage(value: ByteArray, deviceAddress: String) {
        if (value.isEmpty()) return

        val messageType = value[0]
        val data = if (value.size > 1) value.copyOfRange(1, value.size) else ByteArray(0)

        when (messageType) {
            MessageProtocol.TYPE_TEXT -> {
                val message = String(data, Charsets.UTF_8)
                Log.d("BLE", "Received text message: $message")

                val messages = _messagesPerDevice.getOrPut(deviceAddress) { mutableListOf() }
                messages.add(Message.Text(message, false))
                _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }
            }

            MessageProtocol.TYPE_VOICE_START -> {
                Log.d("BLE", "Voice message start received")
                audioBuffer.clear()
            }

            MessageProtocol.TYPE_VOICE_DATA -> {
                Log.d("BLE", "Voice chunk received: ${data.size} bytes")
                audioBuffer.add(data)
            }

            MessageProtocol.TYPE_VOICE_END -> {
                Log.d("BLE", "Voice message end received")
                if (audioBuffer.isNotEmpty()) {
                    val completeAudioData = audioBuffer.reduce { acc, bytes -> acc + bytes }
                    audioBuffer.clear()

                    val messages = _messagesPerDevice.getOrPut(deviceAddress) { mutableListOf() }
                    messages.add(Message.Voice(completeAudioData, false))
                    _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }

                    _receivedAudioData.value = completeAudioData
                    voiceMessageListener?.invoke(completeAudioData, deviceAddress)
                }
            }

            else -> {
                // For backward compatibility, treat as raw text
                val message = String(value, Charsets.UTF_8)
                val messages = _messagesPerDevice.getOrPut(deviceAddress) { mutableListOf() }
                messages.add(Message.Text(message, false))
                _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }
            }
        }
    }

    private fun processSensorData(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        deviceAddress: String
    ) {
        try {
            var sensorType = ""
            var sensorValue = ""
            var unit = ""

            when (characteristic.uuid) {
                BLEConfig.GYROSCOPE_CHARACTERISTIC_UUID -> {
                    sensorType = "GYRO"
                    val x = ByteUtils.bytesToShort(value[0], value[1])
                    val y = ByteUtils.bytesToShort(value[2], value[3])
                    val z = ByteUtils.bytesToShort(value[4], value[5])
                    sensorValue = "$x,$y,$z"
                    unit = "raw"
                }
                BLEConfig.ACCELEROMETER_CHARACTERISTIC_UUID -> {
                    sensorType = "ACCEL"
                    val x = ByteUtils.bytesToShort(value[0], value[1])
                    val y = ByteUtils.bytesToShort(value[2], value[3])
                    val z = ByteUtils.bytesToShort(value[4], value[5])
                    sensorValue = "$x,$y,$z"
                    unit = "raw"
                }
                BLEConfig.MAGNETOMETER_CHARACTERISTIC_UUID -> {
                    sensorType = "MAG"
                    val x = ByteUtils.bytesToShort(value[0], value[1])
                    val y = ByteUtils.bytesToShort(value[2], value[3])
                    val z = ByteUtils.bytesToShort(value[4], value[5])
                    sensorValue = "$x,$y,$z"
                    unit = "raw"
                }
                BLEConfig.AIR_PRESSURE_CHARACTERISTIC_UUID -> {
                    sensorType = "PRESS"
                    val rawPressure = ByteUtils.bytesToInt(value[0], value[1], value[2])
                    val pressure = rawPressure / 4098.0f
                    sensorValue = pressure.toInt().toString()
                    unit = "hPa"
                }
                BLEConfig.TEMPERATURE_CHARACTERISTIC_UUID -> {
                    sensorType = "TEMP"
                    val rawTemp = ByteUtils.bytesToShort(value[0], value[1])
                    val temperature = (rawTemp / 16383.0f) * 165.0f - 40.0f
                    sensorValue = temperature.toInt().toString()
                    unit = "°C"
                }
                BLEConfig.HUMIDITY_CHARACTERISTIC_UUID -> {
                    sensorType = "HUM"
                    val rawHumidity = ByteUtils.bytesToShort(value[0], value[1])
                    val humidity = (rawHumidity / 16383.0f) * 100.0f
                    sensorValue = humidity.toInt().toString()
                    unit = "%"
                }
                BLEConfig.AIR_QUALITY_CHARACTERISTIC_UUID -> {
                    sensorType = "AQ"
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

            if (sensorType.isNotEmpty() && sensorValue.isNotEmpty()) {
                val sensorMessage = "SENSOR:$sensorType=$sensorValue;UNIT=$unit"
                val messages = _messagesPerDevice.getOrPut(deviceAddress) { mutableListOf() }
                messages.add(Message.Text(sensorMessage, false))
                _messagesFlow.value = _messagesPerDevice.mapValues { it.value.toList() }
            }
        } catch (e: Exception) {
            Log.e("BLE", "Error processing sensor data", e)
        }
    }

    private fun processBleModuleScanResult(data: String) {
        try {
            if (data.trim().matches(Regex("\\d+"))) {
                val expectedDeviceCount = data.trim().toInt()
                Log.d("BLE", "Device count: $expectedDeviceCount")
                deviceListBuffer.clear()
                _scannedDevicesFromBle.value = emptyList()

                if (expectedDeviceCount == 0) {
                    _isScanning.value = false
                    scanTimeoutJob?.cancel()
                }
                return
            }

            deviceListBuffer.append(data)
            updateDeviceListPartial()

            if (data.contains('\u0000')) {
                _isScanning.value = false
                scanTimeoutJob?.cancel()
                finalizeDeviceList()
            }
        } catch (e: Exception) {
            Log.e("BLE", "Error processing scan results", e)
            _isScanning.value = false
        }
    }

    private fun processReceivedDeviceList() {
        try {
            _isScanning.value = false
            scanTimeoutJob?.cancel()

            val completeData = deviceListBuffer.toString()
            val deviceNames = completeData.split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.length > 1 && !it.contains('\u0000') }
                .distinct()

            val devices = deviceNames.mapIndexed { index, name ->
                BluetoothDeviceData(
                    name = name,
                    address = "BLE_DEVICE_$index",
                    rssi = null,
                    isConnected = false,
                    isMobileDevice = false,
                    clickCount = 0
                )
            }

            _scannedDevicesFromBle.value = devices
        } catch (e: Exception) {
            Log.e("BLE", "Error processing device list", e)
        }
    }

    private fun updateDeviceListPartial() {
        try {
            val rawData = deviceListBuffer.toString()
            val lines = rawData.split('\n')
            val completeLines = if (lines.isNotEmpty() && !rawData.endsWith('\n')) {
                lines.dropLast(1)
            } else {
                lines
            }

            val deviceNames = completeLines.filter { it.isNotEmpty() }

            if (deviceNames.isNotEmpty()) {
                val devices = deviceNames.mapIndexed { index, name ->
                    BluetoothDeviceData(
                        name = name,
                        address = "BLE_DEVICE_$index",
                        rssi = null,
                        isConnected = false,
                        isMobileDevice = false,
                        clickCount = 0
                    )
                }
                _scannedDevicesFromBle.value = devices
            }
        } catch (e: Exception) {
            Log.e("BLE", "Error updating partial device list", e)
        }
    }

    private fun finalizeDeviceList() {
        try {
            _isScanning.value = false
            scanTimeoutJob?.cancel()

            val rawData = deviceListBuffer.toString()
            val deviceNames = rawData
                .split('\n')
                .map { it.split('\u0000')[0].trim() }
                .filter { it.isNotEmpty() }
                .distinct()

            if (deviceNames.isNotEmpty()) {
                val devices = deviceNames.mapIndexed { index, name ->
                    BluetoothDeviceData(
                        name = name,
                        address = "BLE_DEVICE_$index",
                        rssi = null,
                        isConnected = false,
                        isMobileDevice = false,
                        clickCount = 0
                    )
                }
                _scannedDevicesFromBle.value = devices
            } else {
                _scannedDevicesFromBle.value = emptyList()
            }

            deviceListBuffer.clear()
        } catch (e: Exception) {
            Log.e("BLE", "Error finalizing device list", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeToSensorCharacteristics(gatt: BluetoothGatt?) {
        if (gatt == null) return

        val sensorService = gatt.getService(BLEConfig.SENSOR_SERVICE_UUID)
        if (sensorService == null) {
            Log.e("Sensor", "Sensor service not found")
            return
        }

        pendingSensorIndex = 0
        subscribeTNextSensor(gatt, sensorService)
    }

    @SuppressLint("MissingPermission")
    private fun subscribeTNextSensor(gatt: BluetoothGatt, service: BluetoothGattService) {
        if (pendingSensorIndex >= sensorCharacteristicsList.size) return

        val uuid = sensorCharacteristicsList[pendingSensorIndex]
        val characteristic = service.getCharacteristic(uuid)

        if (characteristic == null) {
            pendingSensorIndex++
            subscribeTNextSensor(gatt, service)
            return
        }

        val success = gatt.setCharacteristicNotification(characteristic, true)
        if (success) {
            val descriptor = characteristic.getDescriptor(BLEConfig.CLIENT_CONFIG_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val writeResult = gatt.writeDescriptor(descriptor)
                if (!writeResult) {
                    pendingSensorIndex++
                    subscribeTNextSensor(gatt, service)
                }
            } else {
                pendingSensorIndex++
                subscribeTNextSensor(gatt, service)
            }
        } else {
            pendingSensorIndex++
            subscribeTNextSensor(gatt, service)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic?
    ) {
        if (characteristic == null) return

        try {
            val descriptor = characteristic.getDescriptor(BLEConfig.CLIENT_CONFIG_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        } catch (e: Exception) {
            Log.e("BLE", "Error enabling notifications", e)
        }
    }

    private fun handleDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        if (descriptor.uuid == BLEConfig.CLIENT_CONFIG_UUID) {
            val characteristicUuid = descriptor.characteristic.uuid

            if (descriptor.value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                if (sensorCharacteristicsList.contains(characteristicUuid)) {
                    pendingUnsubscribeIndex++
                    unsubscribeFromNextCharacteristic(gatt)
                }
                return
            }

            if (sensorCharacteristicsList.contains(characteristicUuid) &&
                descriptor.value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                pendingSensorIndex++
                val service = gatt.getService(BLEConfig.SENSOR_SERVICE_UUID)
                if (service != null) {
                    subscribeTNextSensor(gatt, service)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startUnsubscribeProcess() {
        val gatt = gattClient ?: run {
            completeDisconnect()
            return
        }
        pendingUnsubscribeIndex = 0
        unsubscribeFromNextCharacteristic(gatt)
    }

    @SuppressLint("MissingPermission")
    private fun unsubscribeFromNextCharacteristic(gatt: BluetoothGatt) {
        if (pendingUnsubscribeIndex >= sensorCharacteristicsList.size) {
            completeDisconnect()
            return
        }

        val uuid = sensorCharacteristicsList[pendingUnsubscribeIndex]
        val sensorService = gatt.getService(BLEConfig.SENSOR_SERVICE_UUID)
        val characteristic = sensorService?.getCharacteristic(uuid)

        if (characteristic == null) {
            pendingUnsubscribeIndex++
            unsubscribeFromNextCharacteristic(gatt)
            return
        }

        try {
            gatt.setCharacteristicNotification(characteristic, false)
            val descriptor = characteristic.getDescriptor(BLEConfig.CLIENT_CONFIG_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                val writeResult = gatt.writeDescriptor(descriptor)
                if (!writeResult) {
                    pendingUnsubscribeIndex++
                    unsubscribeFromNextCharacteristic(gatt)
                }
            } else {
                pendingUnsubscribeIndex++
                unsubscribeFromNextCharacteristic(gatt)
            }
        } catch (e: Exception) {
            pendingUnsubscribeIndex++
            unsubscribeFromNextCharacteristic(gatt)
        }
    }

    @SuppressLint("MissingPermission")
    private fun completeDisconnect() {
        try {
            gattClient?.disconnect()
            gattClient?.close()
            gattClient = null
        } catch (e: Exception) {
            Log.e("BLE", "Error closing GATT client", e)
        }

        _isConnected.value = false
        _connectionStatus.value = ConnectionStatus.Disconnected
        writeCharacteristic = null
        notifyCharacteristic = null

        Log.d("BLE", "Disconnect completed")
    }

    sealed class ConnectionStatus {
        object Disconnected : ConnectionStatus()
        object Connecting : ConnectionStatus()
        object Connected : ConnectionStatus()
        data class Error(val message: String) : ConnectionStatus()
    }
}