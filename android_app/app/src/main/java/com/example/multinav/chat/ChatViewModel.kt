package com.example.multinav.chat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multinav.BluetoothService
import com.example.multinav.BluetoothService.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


data class Message(val text: String, val isSentByUser: Boolean)

class ChatViewModel(
    private val deviceAddress: String? = null,
    private val bluetoothService: BluetoothService,
    private val isMobileDevice: Boolean = false // Add this property
) : ViewModel() {
    private val messageQueue = mutableListOf<String>()
    private var isProcessingQueue = false
    private var hasShownFailureMessage = false
    private var isConnecting = false

    private val _connectionState = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionState: StateFlow<ConnectionStatus> = _connectionState

    private val _messages: StateFlow<List<Message>> = bluetoothService.messagesFlow
        .map { messagesMap ->
            messagesMap[deviceAddress] ?: listOf(Message("Welcome to Bluetooth Chat", false))
        }
        .let { mappedFlow ->
            MutableStateFlow(listOf(Message("Welcome to Bluetooth Chat", false))).apply {
                viewModelScope.launch {
                    mappedFlow.collect { messages ->
                        Log.d("ChatViewModel", "Messages updated for device $deviceAddress: $messages")
                        value = messages
                    }
                }
            }
        } as StateFlow<List<Message>>

    val messages: StateFlow<List<Message>> = _messages




    init {
        viewModelScope.launch {
            bluetoothService.bluetoothState.collect { isEnabled ->
                Log.d("ChatViewModel", "Bluetooth state changed: $isEnabled")
                if (isEnabled) {
                    if (!bluetoothService.isConnected.value && !isConnecting) {
                        deviceAddress?.let {
                            receiveMessage("Attempting to connect to device...")
                            connectToDevice(it)
                        }
                    } else if (bluetoothService.isConnected.value) {
                        receiveMessage("Already connected to device")
                    }
                } else {
                    receiveMessage("Please enable Bluetooth")
                    bluetoothService.enableBluetooth()
                }
            }
        }

        viewModelScope.launch {
            try {
                bluetoothService.connectionStatus.collect { status ->
                    if (_connectionState.value != status) {
                        Log.d("ChatViewModel", "Connection status changed: $status")
                        _connectionState.value = status
                        handleConnectionStateChange(status)
                        when (status) {
                            is BluetoothService.ConnectionStatus.Connected ->
                                receiveMessage("Connected to device")
                            is BluetoothService.ConnectionStatus.Disconnected ->
                                receiveMessage("Disconnected from device")
                            is BluetoothService.ConnectionStatus.Error ->
                                receiveMessage("Connection error: ${status.message}")
                            else -> {}
                        }
                        if (status is BluetoothService.ConnectionStatus.Connected) {
                            processMessageQueue()
                        } else if (status is BluetoothService.ConnectionStatus.Disconnected && !isConnecting) {
                            deviceAddress?.let {
                                delay(2000)
                                connectToDevice(it)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error collecting connection status", e)
                _connectionState.value = BluetoothService.ConnectionStatus.Error(
                    "Failed to monitor connection: ${e.message}"
                )
            }
        }

        startMessageListener()
    }

    private fun handleConnectionStateChange(state: BluetoothService.ConnectionStatus) {
        when (state) {
            is BluetoothService.ConnectionStatus.Connected -> {
                hasShownFailureMessage = false
                isConnecting = false
                startMessageListener()
            }
            is BluetoothService.ConnectionStatus.Disconnected -> {
                isConnecting = false
                // Only clear queue if we're not going to reconnect
                if (!bluetoothService.isBluetoothEnabled()) {
                    messageQueue.clear()
                }
            }
            is BluetoothService.ConnectionStatus.Connecting -> {
                isConnecting = true
            }
            else -> {}
        }
    }

    private fun startMessageListener() {
        bluetoothService.startListening { floats, fromDeviceAddress ->
//            Log.d("ChatViewModel", "Raw message received via listener from $fromDeviceAddress")
            // Only process the message if it's from the device associated with this ChatViewModel
            if (fromDeviceAddress == deviceAddress) {
                receiveMessage( floats.toString())            }
        }
    }



    // Helper function to convert 2-byte pairs to floats
    private fun bytesToFloats(bytes: ByteArray): List<Float> {
        if (bytes.size % 2 != 0) {
            Log.w("ChatViewModel", "Byte array length is not a multiple of 2, padding with 0")
            // Pad with a zero byte if the length is odd
            val paddedBytes = bytes + byteArrayOf(0)
            return processBytePairs(paddedBytes)
        }
        return processBytePairs(bytes)
    }

    private fun processBytePairs(bytes: ByteArray): List<Float> {
        val floats = mutableListOf<Float>()
        for (i in bytes.indices step 2) {
            // Ensure we have at least 2 bytes to process
            if (i + 1 < bytes.size) {
                // Combine 2 bytes into a short (16-bit integer)
                val shortValue = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
                // Convert the short to a float (you can adjust the scaling as needed)
                val floatValue = shortValue.toFloat()
                floats.add(floatValue)
            }
        }
        return floats
    }

    fun connectToDevice(address: String) {
        if (isConnecting || bluetoothService.isConnected.value) {
            Log.d("ChatViewModel", "Skipping connect: already connecting or connected")
            return
        }
        viewModelScope.launch {
            try {
                if (!bluetoothService.isBluetoothEnabled()) {
                    bluetoothService.enableBluetooth()
                    receiveMessage("Please enable Bluetooth")
                    return@launch
                }
                isConnecting = true
                _connectionState.value = BluetoothService.ConnectionStatus.Connecting
                receiveMessage("Connecting to device...")

                // Pass isMobileDevice = false since you're likely connecting to a BLE device ("st-bLe99")
                val success = bluetoothService.connectToDevice(address, isMobileDevice = true) //rem
                if (success) {
                    _connectionState.value = BluetoothService.ConnectionStatus.Connected
                    receiveMessage("Connected successfully")
                } else {
                    _connectionState.value = BluetoothService.ConnectionStatus.Error("Connection failed")
                    receiveMessage("Failed to connect to device")
                    delay(2000)
                    connectToDevice(address)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Connection error", e)
                _connectionState.value = BluetoothService.ConnectionStatus.Error(
                    e.message ?: "Unknown error"
                )
                receiveMessage("Connection error: ${e.message}")
                delay(2000)
                connectToDevice(address)
            }
        }
    }
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            try {
                messageQueue.add(message)
                processMessageQueue()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                receiveMessage("Error: ${e.message}")
            }
        }
    }

    private fun processMessageQueue() {
        if (isProcessingQueue || messageQueue.isEmpty()) return
        viewModelScope.launch {
            isProcessingQueue = true
            try {
                while (messageQueue.isNotEmpty()) {
                    if (connectionState.value !is BluetoothService.ConnectionStatus.Connected) {
                        if (!hasShownFailureMessage) {
                            receiveMessage("Failed to send message(s): Not connected")
                            hasShownFailureMessage = true
                        }
                        break
                    }
                    val message = messageQueue.first()
                    val success = bluetoothService.sendMessage(message, isMobileDevice )
                    if (success) {
                        messageQueue.removeAt(0)
                        hasShownFailureMessage = false
                        Log.d("ChatViewModel", "Message sent successfully: $message")
                    } else {
                        Log.e("ChatViewModel", "Failed to send message: $message, will retry")
                        break
                    }
                    delay(100)
                }
            } finally {
                isProcessingQueue = false
                if (messageQueue.isNotEmpty() && connectionState.value is BluetoothService.ConnectionStatus.Connected) {
                    delay(500)
                    processMessageQueue()
                } else if (messageQueue.isNotEmpty() && !isConnecting) {
                    deviceAddress?.let {
                        connectToDevice(it)
                    }
                }
            }
        }
    }

    fun receiveMessage(message: String) {
        viewModelScope.launch {
            Log.d("ChatViewModel", "Adding system message to UI: $message for device: $deviceAddress")
            deviceAddress?.let { address ->
                val currentMessagesMap = bluetoothService.messagesFlow.value.toMutableMap()
                val messages = currentMessagesMap[address]?.toMutableList()
                    ?: mutableListOf(Message("Welcome to Bluetooth Chat", false))
                messages.add(Message(message, false))
                currentMessagesMap[address] = messages.toList()
                (bluetoothService.messagesFlow as MutableStateFlow).value = currentMessagesMap
            }
        }
    }

    fun receiveMessage(floats: List<Float>) {
        viewModelScope.launch {
            Log.d("ChatViewModel", "Processing received floats for device: $deviceAddress")
            deviceAddress?.let { address ->
                val displayMessage = "Floats: [${floats.joinToString(", ")}]"
                val currentMessagesMap = bluetoothService.messagesFlow.value.toMutableMap()
                val messages = currentMessagesMap[address]?.toMutableList()
                    ?: mutableListOf(Message("Welcome to Bluetooth Chat", false))
                messages.add(Message(displayMessage, false))
                currentMessagesMap[address] = messages.toList()
                (bluetoothService.messagesFlow as MutableStateFlow).value = currentMessagesMap
                Log.d("ChatViewModel", "Added float message to UI: $displayMessage for device: $address")
            }
        }
    }

    fun sendVoice() {
        receiveMessage("Voice messages not implemented yet")
    }

    fun makePhoneCall() {
        //receiveMessage("Call functionality not implemented yet")
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothService.disconnect()
            receiveMessage("Disconnected from device")
        }
    }

    override fun onCleared() {
        super.onCleared()

    }


}

class ChatViewModelFactory(
    private val deviceAddress: String? = null,
    private val bluetoothService: BluetoothService,
    private val isMobileDevice: Boolean = false // Add this to pass to ChatViewModel

) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(deviceAddress, bluetoothService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}