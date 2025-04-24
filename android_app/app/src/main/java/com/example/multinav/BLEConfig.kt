package com.example.multinav

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.UUID

object BLEConfig {
    // Nordic UART Service UUIDs
    val CHAT_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val CHARACTERISTIC_UUID_RX: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val CHARACTERISTIC_UUID_TX: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    fun createChatService(): BluetoothGattService {
        val service = BluetoothGattService(CHAT_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // RX Characteristic (Write)
        val rxCharacteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_UUID_RX,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // TX Characteristic (Notify)
        val txCharacteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_UUID_TX,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        // Add descriptor to TX characteristic for enabling notifications
        val descriptor = BluetoothGattDescriptor(
            CLIENT_CONFIG_UUID,
            BluetoothGattCharacteristic.PERMISSION_READ or
                BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        txCharacteristic.addDescriptor(descriptor)

        service.addCharacteristic(rxCharacteristic)
        service.addCharacteristic(txCharacteristic)
        return service
    }
}