package com.example.multinav

data class BluetoothDeviceData(
    val name: String?,
    val address: String,
    val isConnected: Boolean = false
)
