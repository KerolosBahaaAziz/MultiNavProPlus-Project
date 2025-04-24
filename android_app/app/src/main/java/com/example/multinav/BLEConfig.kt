package com.example.multinav


import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import java.util.UUID

object BLEConfig {
    val CHAT_SERVICE_UUID: UUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
    val MESSAGE_CHARACTERISTIC_UUID: UUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b")

    fun createChatService(): BluetoothGattService {
        val service = BluetoothGattService(CHAT_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val messageCharacteristic = BluetoothGattCharacteristic(
            MESSAGE_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        service.addCharacteristic(messageCharacteristic)
        return service
    }
}