package com.example.multinav.model.bluetooth_service

sealed interface BluetoothMessage {
    data class Message(val message: String): BluetoothMessage
    data class Voice(val data: ByteArray): BluetoothMessage
}