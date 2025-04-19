package com.example.multinav

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import android.Manifest // Add this import
import android.util.Log

class BluetoothService(private val context: Context) {
    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var currentSocket: BluetoothSocket? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    // For communication with UI
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Connect to a paired device and navigate to chat screen
    suspend fun connectAndChat(address: String, navigateToChat: () -> Unit): Boolean {
        _connectionStatus.value = ConnectionStatus.Connecting

        val result = withContext(Dispatchers.IO) {
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    _connectionStatus.value = ConnectionStatus.Error("Missing Bluetooth permissions")
                    return@withContext false
                }

                // Cancel any ongoing discovery
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter?.cancelDiscovery()
                }

                val device = bluetoothAdapter?.getRemoteDevice(address)
                if (device == null) {
                    _connectionStatus.value = ConnectionStatus.Error("Device not found")
                    return@withContext false
                }

                // Close any existing connection
                currentSocket?.close()

                try {
                    // Create and connect socket
                    currentSocket = device.createRfcommSocketToServiceRecord(UUID_SPP)
                    currentSocket?.connect()

                    _isConnected.value = true
                    _connectionStatus.value = ConnectionStatus.Connected
                    return@withContext true
                } catch (e: IOException) {
                    // Try fallback method if standard connection fails
                    try {
                        Log.d("BluetoothService", "Trying fallback connection")
                        val socket = device.javaClass.getMethod(
                            "createRfcommSocket", Int::class.java
                        ).invoke(device, 1) as BluetoothSocket

                        currentSocket = socket
                        socket.connect()

                        _isConnected.value = true
                        _connectionStatus.value = ConnectionStatus.Connected
                        return@withContext true
                    } catch (fallbackException: Exception) {
                        Log.e("BluetoothService", "Connection failed", fallbackException)
                        currentSocket?.close()
                        currentSocket = null
                        _isConnected.value = false
                        _connectionStatus.value = ConnectionStatus.Error("Connection failed: ${e.message}")
                        return@withContext false
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothService", "Error connecting", e)
                currentSocket = null
                _isConnected.value = false
                _connectionStatus.value = ConnectionStatus.Error("Error: ${e.message}")
                return@withContext false
            }
        }

        // If connection successful, navigate to chat screen
        if (result) {
            withContext(Dispatchers.Main) {
                navigateToChat()
            }
        }

        return result
    }

    suspend fun sendMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isConnected.value) {
                    return@withContext false
                }

                currentSocket?.outputStream?.write(message.toByteArray())
                currentSocket?.outputStream?.flush()
                return@withContext true
            } catch (e: IOException) {
                Log.e("BluetoothService", "Error sending message", e)
                // If sending fails, update connection status
                if (_isConnected.value) {
                    _isConnected.value = false
                    _connectionStatus.value = ConnectionStatus.Error("Connection lost")
                }
                return@withContext false
            }
        }
    }

    // Receive messages (if needed)
    suspend fun startListening(onMessageReceived: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (_isConnected.value) {
                try {
                    bytes = currentSocket?.inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        withContext(Dispatchers.Main) {
                            onMessageReceived(message)
                        }
                    }
                } catch (e: IOException) {
                    Log.e("BluetoothService", "Error reading", e)
                    break
                }
            }
        }
    }

    fun disconnect() {
        try {
            currentSocket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothService", "Error closing socket", e)
        } finally {
            currentSocket = null
            _isConnected.value = false
            _connectionStatus.value = ConnectionStatus.Disconnected
        }
    }

    // Add these methods to your BluetoothService class

    fun getPairedDevices(): List<BluetoothDeviceData> {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return bluetoothAdapter?.bondedDevices?.map { device ->
            BluetoothDeviceData(
                name = device.name,
                address = device.address,
                isConnected = isDeviceConnected(device)
            )
        } ?: emptyList()
    }

    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return currentSocket?.isConnected == true &&
                currentSocket?.remoteDevice?.address == device.address
    }

    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }



    suspend fun connectToDevice(address: String): Boolean {
        _connectionStatus.value = ConnectionStatus.Connecting

        return withContext(Dispatchers.IO) {
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    _connectionStatus.value = ConnectionStatus.Error("Missing Bluetooth permissions")
                    return@withContext false
                }

                // Cancel any ongoing discovery - this is important as discovery can interfere with connection
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter?.cancelDiscovery()
                }

                val device = bluetoothAdapter?.getRemoteDevice(address)
                if (device == null) {
                    _connectionStatus.value = ConnectionStatus.Error("Device not found")
                    return@withContext false
                }

                // Close any existing connection
                currentSocket?.close()
                currentSocket = null

                // First try the standard connection method
                try {
                    Log.d("BluetoothService", "Attempting standard connection to $address")
                    currentSocket = device.createRfcommSocketToServiceRecord(UUID_SPP)

                    // Set socket timeout to avoid hanging
                    currentSocket?.connect()

                    Log.d("BluetoothService", "Standard connection successful")
                    _isConnected.value = true
                    _connectionStatus.value = ConnectionStatus.Connected
                    return@withContext true
                } catch (e: IOException) {
                    Log.e("BluetoothService", "Standard connection failed: ${e.message}")
                    currentSocket?.close()
                    currentSocket = null

                    // Try fallback method if standard connection fails
                    try {
                        Log.d("BluetoothService", "Attempting fallback connection")
                        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.java)
                        currentSocket = method.invoke(device, 1) as BluetoothSocket

                        // Set socket timeout to avoid hanging
                        currentSocket?.connect()

                        Log.d("BluetoothService", "Fallback connection successful")
                        _isConnected.value = true
                        _connectionStatus.value = ConnectionStatus.Connected
                        return@withContext true
                    } catch (fallbackException: Exception) {
                        Log.e("BluetoothService", "Fallback connection failed: ${fallbackException.message}")
                        currentSocket?.close()
                        currentSocket = null
                        _isConnected.value = false
                        _connectionStatus.value = ConnectionStatus.Error("Connection failed: ${e.message}")
                        return@withContext false
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothService", "Error in connection process: ${e.message}")
                currentSocket = null
                _isConnected.value = false
                _connectionStatus.value = ConnectionStatus.Error("Error: ${e.message}")
                return@withContext false
            }
        }
    }
    // Status class for UI updates
    sealed class ConnectionStatus {
        object Disconnected : ConnectionStatus()
        object Connecting : ConnectionStatus()
        object Connected : ConnectionStatus()
        data class Error(val message: String) : ConnectionStatus()
    }
}

