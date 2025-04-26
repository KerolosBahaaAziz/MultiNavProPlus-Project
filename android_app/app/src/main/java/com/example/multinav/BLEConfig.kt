package com.example.multinav

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.util.Log
import java.util.UUID

object BLEConfig {
    // Service UUID for mobile-to-mobile communication
    val CHAT_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")

    // Service UUID for BLE devices (st-bLe99)
    val CHAT_BLE_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789ABC2000")

    // Characteristic for sending data (write) - for mobile
    val WRITE_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

    // Characteristic for receiving data (notify) - for mobile
    val NOTIFY_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

    // BLE characteristic UUIDs for st-bLe99
    // Both set to "12345678-1234-5678-1234-56789ABC2101" (supports WRITE_NO_RESPONSE and NOTIFY)
    val BLE_WRITE_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789ABC2101")
    val BLE_NOTIFY_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789ABC2101")

    // Client config descriptor UUID (standard for notifications)
    val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    // Function to create GATT service for mobile-to-mobile communication
    fun createChatService(): BluetoothGattService {
        val service = BluetoothGattService(CHAT_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // Write characteristic for mobile devices (write with response)
        val writeCharacteristic = BluetoothGattCharacteristic(
            WRITE_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        Log.d("BLEConfig", "Mobile write characteristic created: $WRITE_CHARACTERISTIC_UUID")

        // Notify characteristic for mobile devices
        val notifyCharacteristic = BluetoothGattCharacteristic(
            NOTIFY_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        Log.d("BLEConfig", "Mobile notify characteristic created: $NOTIFY_CHARACTERISTIC_UUID")

        // Add descriptor for enabling notifications
        val descriptor = BluetoothGattDescriptor(
            CLIENT_CONFIG_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        notifyCharacteristic.addDescriptor(descriptor)

        service.addCharacteristic(writeCharacteristic)
        service.addCharacteristic(notifyCharacteristic)
        Log.d("BLEConfig", "Mobile service created with ${service.characteristics.size} characteristics")
        return service
    }

    // Function to create GATT service for BLE devices (st-bLe99)
    fun createBLEChatService(): BluetoothGattService {
        val service = BluetoothGattService(CHAT_BLE_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // Write characteristic for BLE devices (write without response)
        val writeCharacteristic = BluetoothGattCharacteristic(
            BLE_WRITE_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        Log.d("BLEConfig", "BLE write characteristic created: $BLE_WRITE_CHARACTERISTIC_UUID")

        // Notify characteristic for BLE devices
        val notifyCharacteristic = BluetoothGattCharacteristic(
            BLE_NOTIFY_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        Log.d("BLEConfig", "BLE notify characteristic created: $BLE_NOTIFY_CHARACTERISTIC_UUID")

        // Add descriptor for enabling notifications
        val descriptor = BluetoothGattDescriptor(
            CLIENT_CONFIG_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        notifyCharacteristic.addDescriptor(descriptor)

        service.addCharacteristic(writeCharacteristic)
        service.addCharacteristic(notifyCharacteristic)
        Log.d("BLEConfig", "BLE service created with ${service.characteristics.size} characteristics")
        return service
    }
}