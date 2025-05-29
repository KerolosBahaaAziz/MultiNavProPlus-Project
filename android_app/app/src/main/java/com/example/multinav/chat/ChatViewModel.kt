    package com.example.multinav.chat

    import android.media.MediaRecorder
    import android.util.Log
    import androidx.compose.runtime.State
    import androidx.compose.runtime.mutableStateOf
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.viewModelScope
    import com.example.multinav.model.AudioRecorder
    import com.example.multinav.model.bluetooth_service.BluetoothService
    import com.example.multinav.model.bluetooth_service.BluetoothService.*
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.map
    import kotlinx.coroutines.launch
    import java.io.File

    sealed class Message {
        data class Text(
            val text: String,
            val isSentByUser: Boolean,
            val isSentSuccessfully: Boolean = true,
        ) : Message()

        data class Voice(val audioBytes: ByteArray, val isSentByUser: Boolean) : Message()
    }

    class ChatViewModel(
        private val deviceAddress: String,
        private val bluetoothService: BluetoothService,
        private val audioRecorder: AudioRecorder
    ) : ViewModel() {

        private val _connectionState = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
        val connectionState: StateFlow<ConnectionStatus> = _connectionState

        private val _messages: StateFlow<List<Message>> = bluetoothService.messagesFlow
            .map { messagesMap ->
                messagesMap[deviceAddress] ?: emptyList()
            }
            .let { mappedFlow ->
                MutableStateFlow<List<Message>>(emptyList()).apply {
                    viewModelScope.launch {
                        mappedFlow.collect { messages ->
                            Log.d("ChatViewModel", "Messages updated for device $deviceAddress: ${messages.size} messages")
                            value = messages
                        }
                    }
                }
            } as StateFlow<List<Message>>

        val messages: StateFlow<List<Message>> = _messages

        private val _isRecording = mutableStateOf(false)
        val isRecording: State<Boolean> = _isRecording

        init {
            // Monitor Bluetooth state
            viewModelScope.launch {
                bluetoothService.bluetoothState.collect { isEnabled ->
                    Log.d("ChatViewModel", "Bluetooth state changed: $isEnabled")
                    if (!isEnabled) {
                        addSystemMessage("Please enable Bluetooth")
                        bluetoothService.enableBluetooth()
                    }
                }
            }

            // Monitor connection status
            viewModelScope.launch {
                bluetoothService.connectionStatus.collect { status ->
                    Log.d("ChatViewModel", "Connection status changed: $status")
                    _connectionState.value = status

                    when (status) {
                        is ConnectionStatus.Connected -> {
                            addSystemMessage("Connected to device")
                            startMessageListener()
                        }
                        is ConnectionStatus.Disconnected -> {
                            addSystemMessage("Disconnected from device")
                        }
                        is ConnectionStatus.Error -> {
                            addSystemMessage("Connection error: ${status.message}")
                        }
                        is ConnectionStatus.Connecting -> {
                            addSystemMessage("Connecting...")
                        }
                    }
                }
            }

            // Observe received audio data
            viewModelScope.launch {
                bluetoothService.receivedAudioData.collect { audioBytes ->
                    if (audioBytes.isNotEmpty() && deviceAddress == bluetoothService.getConnectedDeviceAddress()) {
                        // Audio is already added to messages by BluetoothService
                        Log.d("ChatViewModel", "Received audio data: ${audioBytes.size} bytes")
                    }
                }
            }

            startMessageListener()
        }

        private fun startMessageListener() {
            bluetoothService.startListening { floats, fromDeviceAddress ->
                if (fromDeviceAddress == deviceAddress) {
                    Log.d("ChatViewModel", "Received float data from $deviceAddress: $floats")
                    // Float data is already added to messages by BluetoothService
                }
            }
        }

        fun sendMessage(message: String) {
            if (message.isBlank()) return

            viewModelScope.launch {
                try {
                    val trimmedMessage = message.trim()

                    // Check message length for RFOXIA CHAT (50 byte limit minus protocol header)
                    if (trimmedMessage.toByteArray(Charsets.UTF_8).size > 49) {
                        addSystemMessage("Message too long! Max 49 characters")
                        return@launch
                    }

                    // Add message to UI immediately
                    val messages = (_messages.value as? MutableList<Message>)?.toMutableList() ?: mutableListOf()
                    messages.add(Message.Text(trimmedMessage, true, false))
                    (_messages as MutableStateFlow).value = messages

                    // Send via Bluetooth
                    val success = bluetoothService.sendTextMessage(trimmedMessage)

                    if (!success) {
                        addSystemMessage("Failed to send message")
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error sending message", e)
                    addSystemMessage("Error: ${e.message}")
                }
            }
        }

        fun sendVoiceMessage() {
            if (!audioRecorder.isRecording()) return

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val audioBytes = audioRecorder.stopRecording()
                    _isRecording.value = false

                    if (audioBytes.isNotEmpty()) {
                        // Add voice message to UI
                        viewModelScope.launch(Dispatchers.Main) {
                            val messages = (_messages.value as? MutableList<Message>)?.toMutableList() ?: mutableListOf()
                            messages.add(Message.Voice(audioBytes, true))
                            (_messages as MutableStateFlow).value = messages
                        }

                        // Send via Bluetooth
                        val success = bluetoothService.sendVoiceMessage(audioBytes)
                        if (success) {
                            Log.d("ChatViewModel", "Voice message sent successfully")
                        } else {
                            Log.e("ChatViewModel", "Failed to send voice message")
                            addSystemMessage("Failed to send voice message")
                        }
                    } else {
                        addSystemMessage("No audio recorded")
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error sending voice message", e)
                    addSystemMessage("Error: ${e.message}")
                    _isRecording.value = false
                }
            }
        }

        fun startRecording() {
            if (audioRecorder.isRecording()) return

            val success = audioRecorder.startRecording()
            if (success) {
                _isRecording.value = true
            } else {
                addSystemMessage("Failed to start recording")
            }
        }

        fun cancelRecording() {
            if (!audioRecorder.isRecording()) return

            audioRecorder.cancelRecording()
            _isRecording.value = false
        }

        private fun addSystemMessage(message: String) {
            viewModelScope.launch {
                Log.d("ChatViewModel", "Adding system message: $message")
                val messages = (_messages.value as? MutableList<Message>)?.toMutableList() ?: mutableListOf()
                messages.add(Message.Text(message, false))
                (_messages as MutableStateFlow).value = messages
            }
        }

        fun disconnect() {
            viewModelScope.launch {
                bluetoothService.disconnect()
            }
        }

        override fun onCleared() {
            super.onCleared()
            if (_isRecording.value) {
                audioRecorder.cancelRecording()
            }
        }
    }

    class ChatViewModelFactory(
        private val deviceAddress: String,
        private val bluetoothService: BluetoothService,
        private val audioRecorder: AudioRecorder
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(deviceAddress, bluetoothService, audioRecorder) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }