package com.example.multinav.model.bluetooth_service

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDeviceData> = emptyList(),
    val scannedDevicesFromBle:List<BluetoothDeviceData> = emptyList(),
    val pairedDevices: List<BluetoothDeviceData> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<BluetoothMessage> = emptyList(),
    val isBluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val isBleModuleScanning: Boolean = false, // Add this field
    val statusMessage: String? = null,  // Add status message for feedback
    val scanCompleted: Boolean = false  // Add flag for scan completion
)