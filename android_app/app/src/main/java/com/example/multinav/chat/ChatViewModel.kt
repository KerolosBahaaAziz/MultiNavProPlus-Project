package com.example.multinav.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multinav.BluetoothService
import com.example.multinav.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Message(val text: String, val isSentByUser: Boolean)

class ChatViewModel(
    private val deviceAddress: String? = null,
    private val bluetoothService: BluetoothService
) : ViewModel() {
    private var hasReceivedAck = false
    private var hasSentAck = false

    private fun handleIncomingMessage(message: String) {
        when (message) {
            "BLE:ACK_CONNECT" -> {
                if (!hasReceivedAck) {
                    hasReceivedAck = true
                    receiveMessage("Partner device connected")
                    if (!hasSentAck) {
                        // Send ACK back if not sent yet
                        sendMessage("BLE:ACK_CONNECT")
                        hasSentAck = true
                    }
                }
            }
            else -> receiveMessage(message)
        }
    }


    fun disconnect() {
        viewModelScope.launch {
            // Send disconnect message before closing
            sendMessage("BLE:ACK_DISCONNECT")
            bluetoothService.disconnect()
            hasReceivedAck = false
            hasSentAck = false
            receiveMessage("Disconnected from device")
        }
    }


    // Reset ACK states when connection changes
    private fun handleConnectionStateChange(state: ConnectionState) {
        when (state) {
            is ConnectionState.Connected -> {
                // State will be reset when connection is established
                hasReceivedAck = false
                hasSentAck = false
            }
            is ConnectionState.Disconnected -> {
                hasReceivedAck = false
                hasSentAck = false
            }
            else -> {} // No action needed for other states
        }
    }

    private val _messages = MutableStateFlow<List<Message>>(
        listOf(
            Message("Welcome to Bluetooth Chat", false)
        )
    )
    val messages: StateFlow<List<Message>> = _messages

    // Add ConnectionState enum and property
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    init {
        // Monitor Bluetooth connection state
        viewModelScope.launch {
            bluetoothService.connectionStatus.collect { status ->
                _connectionState.value = when (status) {
                    is BluetoothService.ConnectionStatus.Connected -> ConnectionState.Connected
                    is BluetoothService.ConnectionStatus.Connecting -> ConnectionState.Connecting
                    is BluetoothService.ConnectionStatus.Error -> ConnectionState.Error(status.message)
                    else -> ConnectionState.Disconnected
                }
            }
        }// Add a message about attempting connection
        receiveMessage("Attempting to connect to device...")

        // Start listening for incoming messages
        startMessageListener()

        // Try to connect if not already connected
        if (!bluetoothService.isConnected.value) {
            deviceAddress?.let { connectToDevice(it) }
        } else {
            receiveMessage("Already connected to device")
        }


    }

    private fun startMessageListener() {
        viewModelScope.launch {
            try {
                bluetoothService.startListening { receivedMessage ->
                    handleIncomingMessage(receivedMessage)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error in message listener", e)
                receiveMessage("Error receiving messages: ${e.message}")
            }
        }
    }


    fun connectToDevice(address: String) {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting
                receiveMessage("Connecting to device...")

                val success = bluetoothService.connectToDevice(address)

                if (success) {
                    _connectionState.value = ConnectionState.Connected
                    receiveMessage("Connected successfully")
                } else {
                    _connectionState.value = ConnectionState.Error("Connection failed")
                    receiveMessage("Failed to connect to device")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Connection error", e)
                _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
                receiveMessage("Connection error: ${e.message}")
            }
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            try {
                // Add message to UI
                _messages.value = _messages.value + Message(message, true)

                if (connectionState.value is ConnectionState.Connected) {
                    val success = bluetoothService.sendMessage(message)
                    if (!success) {
                        receiveMessage("Failed to send BLE command")
                    }
                } else {
                    receiveMessage("Not connected to BLE device")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending BLE command", e)
                receiveMessage("Error: ${e.message}")
            }
        }
    }


    fun receiveMessage(message: String) {
        viewModelScope.launch {
            _messages.value = _messages.value + Message(message, false)
        }
    }

    fun sendVoice() {
        // Placeholder for voice functionality
        receiveMessage("Voice messages not implemented yet")
    }

    fun makePhoneCall() {
        // Placeholder for call functionality
        receiveMessage("Call functionality not implemented yet")
    }




    override fun onCleared() {
        super.onCleared()
        bluetoothService.disconnect()
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