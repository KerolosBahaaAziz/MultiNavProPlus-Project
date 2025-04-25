package com.example.multinav

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.util.Log
import java.util.UUID

object BLEConfig {
    // Service UUID
   // val CHAT_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")

    val CHAT_SERVICE_UUID: UUID = UUID.fromString("0020BC9A-7856-3412-7856-341278563412")

    // Characteristic for sending data (write)
    val WRITE_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

    // Characteristic for receiving data (notify)
    val NOTIFY_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

    // Client config descriptor UUID (for notifications)
    val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")



    fun createChatService(): BluetoothGattService {
        val service = BluetoothGattService(CHAT_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // Write characteristic (for sending data)
        val writeCharacteristic = BluetoothGattCharacteristic(
            WRITE_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        Log.d("BLEConfig", "Write characteristic created: $WRITE_CHARACTERISTIC_UUID")

        // Notify characteristic (for receiving data)
        val notifyCharacteristic = BluetoothGattCharacteristic(
            NOTIFY_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        Log.d("BLEConfig", "Notify characteristic created: $NOTIFY_CHARACTERISTIC_UUID")

        // Add descriptor for enabling notifications
        val descriptor = BluetoothGattDescriptor(
            CLIENT_CONFIG_UUID,
            BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        notifyCharacteristic.addDescriptor(descriptor)

        service.addCharacteristic(writeCharacteristic)
        service.addCharacteristic(notifyCharacteristic)
        Log.d("BLEConfig", "Service created with ${service.characteristics.size} characteristics")
        return service
    }
}