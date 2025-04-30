package com.example.multinav.chat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multinav.BluetoothService
import com.example.multinav.BluetoothService.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files.delete

sealed class Message {
    data class Text(val text: String, val isSentByUser: Boolean, val isSentSuccessfully: Boolean = true) : Message()
    data class Voice(val audioBytes: ByteArray, val isSentByUser: Boolean) : Message()
}

class ChatViewModel(
    private val deviceAddress: String? = null,
    private val bluetoothService: BluetoothService,
    private val isMobileDevice: Boolean = false
) : ViewModel() {
    private val messageQueue = mutableListOf<String>()
    private var isProcessingQueue = false
    private var hasShownFailureMessage = false
    private var isConnecting = false

    private val _connectionState = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionState: StateFlow<ConnectionStatus> = _connectionState

    private val _messages: StateFlow<List<Message>> = bluetoothService.messagesFlow
        .map { messagesMap ->
            messagesMap[deviceAddress] ?: listOf<Message>(Message.Text("Welcome to Bluetooth Chat", false))
        }
        .let { mappedFlow ->
            MutableStateFlow<List<Message>>(listOf(Message.Text("Welcome to Bluetooth Chat", false))).apply {
                viewModelScope.launch {
                    mappedFlow.collect { messages ->
                        Log.d("ChatViewModel", "Messages updated for device $deviceAddress: $messages")
                        value = messages
                    }
                }
            }
        } as StateFlow<List<Message>>

    val messages: StateFlow<List<Message>> = _messages

    private val _isRecording = mutableStateOf(false)
    val isRecording: State<Boolean> = _isRecording

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

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
            // Only process the message if it's from the device associated with this ChatViewModel
            if (fromDeviceAddress == deviceAddress) {
                receiveMessage(floats)
            }
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

                val success = bluetoothService.connectToDevice(address, isMobileDevice = isMobileDevice)
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
                // Add the message to the UI immediately (before sending over Bluetooth)
                deviceAddress?.let { address ->
                    bluetoothService.addMessage(address, Message.Text(message, true, isSentSuccessfully = false))
                }
                // Add to queue for actual Bluetooth sending
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
                    val success = bluetoothService.sendMessage(message, isMobileDevice)
                    if (success) {
                        // Update the message in the UI to mark it as sent successfully
                        deviceAddress?.let { address ->
                            bluetoothService.addMessage(
                                address,
                                Message.Text(message, true, isSentSuccessfully = true)
                            )
                        }
                        messageQueue.removeAt(0)
                        hasShownFailureMessage = false
                        Log.d("ChatViewModel", "Message sent successfully: $message")
                    } else {
                        Log.e("ChatViewModel", "Failed to send message: $message, will retry")
                        receiveMessage("Failed to send message: $message")
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
                bluetoothService.addMessage(address, Message.Text(message, false))
            }
        }
    }

    fun receiveMessage(floats: List<Float>) {
        viewModelScope.launch {
            Log.d("ChatViewModel", "Processing received floats for device: $deviceAddress")
            deviceAddress?.let { address ->
                val displayMessage = "Floats: [${floats.joinToString(", ")}]"
                bluetoothService.addMessage(address, Message.Text(displayMessage, false))
                Log.d("ChatViewModel", "Added float message to UI: $displayMessage for device: $address")
            }
        }
    }

    fun sendVoice(audioBytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            if (audioBytes.isNotEmpty()) {
                // Add the voice message to the UI immediately
                deviceAddress?.let { address ->
                    bluetoothService.addMessage(address, Message.Voice(audioBytes, true))
                }

                // Send the voice message
                val success = bluetoothService.sendVoiceMessage(audioBytes)
                if (success) {
                    Log.d("ChatViewModel", "Voice message sent successfully")
                } else {
                    Log.e("ChatViewModel", "Failed to send voice message")
                    receiveMessage("Failed to send voice message")
                }
            }
        }
    }

    fun setRecordingState(isRecording: Boolean) {
        _isRecording.value = isRecording
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
        // Stop recording if the ViewModel is cleared
        if (_isRecording.value) {
            mediaRecorder?.apply {
                stop()
                release()
            }
            audioFile?.delete()
        }
    }
}

class ChatViewModelFactory(
    private val deviceAddress: String? = null,
    private val bluetoothService: BluetoothService,
    private val isMobileDevice: Boolean = false
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(deviceAddress, bluetoothService, isMobileDevice) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}