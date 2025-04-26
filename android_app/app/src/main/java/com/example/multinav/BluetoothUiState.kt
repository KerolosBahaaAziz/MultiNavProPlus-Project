package com.example.multinav

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDeviceData> = emptyList(),
    val pairedDevices: List<BluetoothDeviceData> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<BluetoothMessage> = emptyList(),
  val isBluetoothEnabled: Boolean = false,

val isScanning: Boolean = false
)