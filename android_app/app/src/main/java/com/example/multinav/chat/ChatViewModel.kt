package com.example.multinav.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multinav.BluetoothService
import com.example.multinav.BluetoothService.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


data class Message(val text: String, val isSentByUser: Boolean)

class ChatViewModel(
    private val deviceAddress: String? = null,
    private val bluetoothService: BluetoothService
) : ViewModel() {
    private val messageQueue = mutableListOf<String>()
    private var isProcessingQueue = false
    private var hasShownFailureMessage = false
    private var isConnecting = false

    private val _connectionState = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionState: StateFlow<ConnectionStatus> = _connectionState



    private val _messages = MutableStateFlow<List<Message>>(
        listOf(Message("Welcome to Bluetooth Chat", false))
    )
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
        Log.d("ChatViewModel", "Starting message listener")
        bluetoothService.startListening { receivedMessage ->
            Log.d("ChatViewModel", "Message received via listener: $receivedMessage")
            receiveMessage(receivedMessage)
        }
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
                val success = bluetoothService.connectToDevice(address, isMobileDevice = false)
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
                _messages.value = _messages.value + Message(message, true)
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
                    val success = bluetoothService.sendMessage(message)
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
            Log.d("ChatViewModel", "Adding received message to UI: $message")
            _messages.value = _messages.value + Message(message, false)
        }
    }

    fun sendVoice() {
        receiveMessage("Voice messages not implemented yet")
    }

    fun makePhoneCall() {
        receiveMessage("Call functionality not implemented yet")
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothService.disconnect()
            receiveMessage("Disconnected from device")
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothService.disconnect()
        bluetoothService.cleanup()
    }


}

class ChatViewModelFactory(
    private val deviceAddress: String? = null,
    private val bluetoothService: BluetoothService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(deviceAddress, bluetoothService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}