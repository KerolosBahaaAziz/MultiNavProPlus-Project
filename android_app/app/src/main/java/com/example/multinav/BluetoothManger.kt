//@file:Suppress("UNREACHABLE_CODE")
//
//package com.example.multinav
//
//import android.Manifest
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothSocket
//import android.content.pm.PackageManager
//import androidx.core.app.ActivityCompat
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import java.util.UUID
//
//class BluetoothManger {
//    private lateinit var bluetoothAdapter: BluetoothAdapter
//    private var socket: BluetoothSocket? = null
//
//
//    fun initialize() {
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//    }
//
//    suspend fun connectToDevice(address: String): Boolean {
//        return withContext(Dispatchers.IO) {
//            try {
//                val device = bluetoothAdapter.getRemoteDevice(address)
//                if (ActivityCompat.checkSelfPermission(
//                        this,
//                        Manifest.permission.BLUETOOTH_CONNECT
//                    ) != PackageManager.PERMISSION_GRANTED
//                )
//                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("YOUR-UUID"))
//                socket?.connect()
//                true
//            } catch (e: Exception) {
//                false
//            }
//        }
//    }
//
//    fun sendMessage(message: String) {
//        socket?.outputStream?.write(message.toByteArray())
//    }
//
//    fun sendVoice(audioData: ByteArray) {
//        socket?.outputStream?.write(audioData)
//    }
//}
//}