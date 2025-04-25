package com.example.multinav.chat

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multinav.BluetoothService
import com.example.multinav.ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Message(val text: String, val isSentByUser: Boolean)

class ChatViewModel(
    private val deviceAddress: String? = null,
    private val bluetoothService: BluetoothService
) : ViewModel() {

    // Add a message queue for reliability
    private val messageQueue = mutableListOf<String>()
    private var isProcessingQueue = false
    private var hasShownFailureMessage = false // Flag to prevent spamming failure messages


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
                hasShownFailureMessage = false // Reset failure message flag on reconnect

                // Reinitialize listener to ensure it's set
                startMessageListener()

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
                val newState = when (status) {
                    is BluetoothService.ConnectionStatus.Connected -> ConnectionState.Connected
                    is BluetoothService.ConnectionStatus.Connecting -> ConnectionState.Connecting
                    is BluetoothService.ConnectionStatus.Error -> ConnectionState.Error(status.message)
                    else -> ConnectionState.Disconnected
                }

                // Only update if state changed
                if (_connectionState.value != newState) {
                    _connectionState.value = newState
                    handleConnectionStateChange(newState)

                    // Add status message to chat
                    when (newState) {
                        is ConnectionState.Connected -> receiveMessage("Connected to device")
                        is ConnectionState.Disconnected -> receiveMessage("Disconnected from device")
                        is ConnectionState.Error -> receiveMessage("Connection error: ${(newState as ConnectionState.Error).message}")
                        else -> {} // No message for connecting state
                    }

                    // Try to process queue when connected
                    if (newState is ConnectionState.Connected) {
                        processMessageQueue()
                    }
                }
            }
        }

        // Start listening for incoming messages
        startMessageListener()

        // Try to connect if not already connected
        if (!bluetoothService.isConnected.value) {
            deviceAddress?.let {
                receiveMessage("Attempting to connect to device...")
                connectToDevice(it)
            }
        } else {
            receiveMessage("Already connected to device")
            // Send ACK if already connected
            sendMessage("BLE:ACK_CONNECT")
        }
    }

    private fun startMessageListener() {
        Log.d("ChatViewModel", "Starting message listener")
        bluetoothService.startListening { receivedMessage ->
            Log.d("ChatViewModel", "Message received via listener: $receivedMessage")
            handleIncomingMessage(receivedMessage)
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

                // Add to queue and process
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
                    if (connectionState.value !is ConnectionState.Connected) {
                        // Stop processing if not connected and show failure message only once
                        if (!hasShownFailureMessage) {
                            receiveMessage("Failed to send message(s): Not connected")
                            hasShownFailureMessage = true
                        }
                        break
                    }

                    val message = messageQueue.first()
                    val success = bluetoothService.sendMessage(message)

                    if (success) {
                        // Remove from queue if sent successfully
                        messageQueue.removeAt(0)
                        hasShownFailureMessage = false // Reset on successful send
                    } else {
                        // Stop processing on failure and show message only once
                        if (!hasShownFailureMessage) {
                            receiveMessage("Failed to send message")
                            hasShownFailureMessage = true
                        }
                        break
                    }

                    // Small delay between messages
                    delay(100)
                }
            } finally {
                isProcessingQueue = false

                // If there are still messages and we're connected, retry after a longer delay
                // Use a longer delay to avoid spamming attempts
                if (messageQueue.isNotEmpty() && connectionState.value is ConnectionState.Connected) {
                    delay(5000) // Wait longer (5 seconds) before retrying to avoid spamming
                    processMessageQueue()
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