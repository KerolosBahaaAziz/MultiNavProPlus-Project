import android.content.Context
import android.content.Intent
import android.provider.Settings

//package com.example.multinav
//
//    import android.annotation.SuppressLint
//    import android.bluetooth.*
//    import android.content.BroadcastReceiver
//    import android.content.Context
//    import android.content.Intent
//    import android.content.IntentFilter
//    import android.content.pm.PackageManager
//    import android.Manifest
//    import android.util.Log
//    import kotlinx.coroutines.flow.MutableStateFlow
//    import kotlinx.coroutines.flow.asStateFlow
//    import kotlinx.coroutines.flow.update
//
//    class BluetoothService(
//        private val context: Context
//    ) {
//        private val TAG = "Bluetooth"
//
//
//        // Add StateFlow for scanned devices
//        private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceData>>(emptyList())
//        val scannedDevices = _scannedDevices.asStateFlow()
//
//        private val bluetoothManager by lazy {
//            context.getSystemService(BluetoothManager::class.java)
//        }
//        private val bluetoothAdapter by lazy {
//            bluetoothManager?.adapter
//        }
//
//        private var currentServerSocket: BluetoothServerSocket? = null
//        private var currentClientSocket: BluetoothSocket? = null
//
//        // Add broadcast receiver for discovered devices
//        private val receiver = object : BroadcastReceiver() {
//            @SuppressLint("MissingPermission")
//            override fun onReceive(context: Context?, intent: Intent?) {
//                when(intent?.action) {
//                    BluetoothDevice.ACTION_FOUND -> {
//                        val device = intent.getParcelableExtra<BluetoothDevice>(
//                            BluetoothDevice.EXTRA_DEVICE
//                        )
//                        device?.let {
//                            // Log discovered device
//                            Log.d(TAG, "Device found - Name: ${it.name}, Address: ${it.address}")
//
//                            val newDevice = BluetoothDeviceData(
//                                name = it.name,
//                                address = it.address
//                            )
//                            _scannedDevices.update { devices ->
//                                if (devices.none { existingDevice ->
//                                        existingDevice.address == newDevice.address
//                                    }) {
//                                    Log.d(TAG, "Adding new device to list: ${newDevice.name}")
//                                    devices + newDevice
//                                } else devices
//                            }
//                        }
//                    }
//                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
//                        Log.d(TAG, "Discovery started")
//                    }
//                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
//                        Log.d(TAG, "Discovery finished")
//                    }
//                }
//            }
//        }
//        init {
//            // Register receiver for device discovery and state changes
//            context.registerReceiver(
//                receiver,
//                IntentFilter().apply {
//                    addAction(BluetoothDevice.ACTION_FOUND)
//                    addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
//                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//                }
//            )
//        }
//
//        @SuppressLint("MissingPermission")
//        fun pairDevice(device: BluetoothDeviceData): Boolean {
//            return try {
//                val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
//                if (bluetoothDevice?.bondState != BluetoothDevice.BOND_BONDED) {
//                    bluetoothDevice?.createBond()
//                    return bluetoothDevice?.bondState == BluetoothDevice.BOND_BONDED
//                }
//                true
//            } catch (e: Exception) {
//                Log.e(TAG, "Pairing failed: ${e.message}")
//                false
//            }
//        }
//
//
//        @SuppressLint("MissingPermission")
//        fun startDiscovery() {
//            Log.d(TAG, "Starting discovery")
//            _scannedDevices.value = emptyList()
//            bluetoothAdapter?.startDiscovery()
//        }
//
//        @SuppressLint("MissingPermission")
//        fun stopDiscovery() {
//            Log.d(TAG, "Stopping discovery")
//            bluetoothAdapter?.cancelDiscovery()
//        }
//
//
//        @SuppressLint("MissingPermission")
//        fun getPairedDevices(): List<BluetoothDeviceData> {
//            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//                return emptyList()
//            }
//            return bluetoothAdapter
//                ?.bondedDevices
//                ?.map { device ->
//                    BluetoothDeviceData(
//                        name = device.name,
//                        address = device.address
//                    )
//                }
//                ?: emptyList()
//        }
//
//        private fun hasPermission(permission: String): Boolean {
//            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
//        }
//
//        // Add cleanup method
//        fun release() {
//            context.unregisterReceiver(receiver)
//        }
//    }


class BluetoothService(
    private val context: Context
) {
    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}