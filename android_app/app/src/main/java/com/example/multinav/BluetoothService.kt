package com.example.multinav

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class BluetoothService(private val context: Context) {
    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var gattServer: BluetoothGattServer? = null
    private var gattClient: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false
    private var isAdvertising = false

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private var messageListener: ((String) -> Unit)? = null
    private val scanResults = mutableMapOf<String, BluetoothDevice>()


    private fun sendConnectionAck() {
        viewModelScope.launch {
            sendMessage("BLE:ACK_CONNECT")
        }
    }


    fun startScanning() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

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

    fun stopScanning() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (!isScanning) return

        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
        Log.d("BLE", "Stopped scanning")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }

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


    fun startAdvertising() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADVERTISE)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (isAdvertising) return

        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(BLEConfig.CHAT_SERVICE_UUID))
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
        isAdvertising = true
        Log.d("BLE", "Started advertising")
    }

    fun stopAdvertising() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADVERTISE)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (!isAdvertising) return

        bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        isAdvertising = false
        Log.d("BLE", "Stopped advertising")
    }


    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d("BLE", "Advertising started")
            startGattServer()
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLE", "Advertising failed: $errorCode")
            _connectionStatus.value = ConnectionStatus.Error("Failed to start advertising")
        }
    }

    private fun startGattServer() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
        gattServer?.addService(BLEConfig.createChatService())
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _isConnected.value = true
                _connectionStatus.value = ConnectionStatus.Connected

                sendConnectionAck()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _isConnected.value = false
                _connectionStatus.value = ConnectionStatus.Disconnected
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
            if (characteristic.uuid == BLEConfig.CHARACTERISTIC_UUID_RX) {
                val message = String(value)
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
            if (descriptor.uuid == BLEConfig.CLIENT_CONFIG_UUID) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }
    }

    suspend fun connectToDevice(address: String): Boolean {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                _connectionStatus.value = ConnectionStatus.Connecting
                val device = bluetoothAdapter?.getRemoteDevice(address)
                if (device == null) {
                    _connectionStatus.value = ConnectionStatus.Error("Device not found")
                    return@withContext false
                }

                gattClient?.close()
                gattClient = device.connectGatt(context, false, gattClientCallback)
                true
            } catch (e: Exception) {
                Log.e("BLE", "Connection error", e)
                _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Unknown error")
                false
            }
        }
    }

    private val gattClientCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _isConnected.value = true
                    _connectionStatus.value = ConnectionStatus.Connected
                    gatt.discoverServices()

                    sendConnectionAck()

                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _isConnected.value = false
                    _connectionStatus.value = ConnectionStatus.Disconnected
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(BLEConfig.CHAT_SERVICE_UUID)
                rxCharacteristic = service?.getCharacteristic(BLEConfig.CHARACTERISTIC_UUID_RX)
                txCharacteristic = service?.getCharacteristic(BLEConfig.CHARACTERISTIC_UUID_TX)

                // Enable notifications for TX characteristic
                enableNotifications(gatt, txCharacteristic)
            }
        }
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == BLEConfig.CHARACTERISTIC_UUID_TX) {
                val message = String(value)
                messageListener?.invoke(message)
            }
        }
    }



    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        characteristic?.let {
            gatt.setCharacteristicNotification(it, true)
            val descriptor = it.getDescriptor(BLEConfig.CLIENT_CONFIG_UUID)
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    suspend fun sendMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isConnected.value) return@withContext false

                rxCharacteristic?.let { characteristic ->
                    characteristic.setValue(message.toByteArray())
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                        return@withContext false
                    }
                    gattClient?.writeCharacteristic(characteristic)
                    true
                } ?: false
            } catch (e: Exception) {
                Log.e("BLE", "Send message error", e)
                false
            }
        }
    }

    fun startListening(listener: (String) -> Unit) {
        messageListener = listener
    }

    fun getPairedDevices(): List<BluetoothDeviceData> {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return bluetoothAdapter?.bondedDevices?.map { device ->
            BluetoothDeviceData(
                name = device.name,
                address = device.address,
                isConnected = isDeviceConnected(device)
            )
        } ?: emptyList()
    }

    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return _isConnected.value && gattClient?.device?.address == device.address
    }

    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun disconnect() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        gattClient?.disconnect()
        gattClient?.close()
        gattServer?.close()
        _isConnected.value = false
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    sealed class ConnectionStatus {
        object Disconnected : ConnectionStatus()
        object Connecting : ConnectionStatus()
        object Connected : ConnectionStatus()
        data class Error(val message: String) : ConnectionStatus()
    }
}