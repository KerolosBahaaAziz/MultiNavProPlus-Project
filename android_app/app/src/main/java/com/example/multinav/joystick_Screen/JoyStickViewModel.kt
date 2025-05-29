package com.example.joystick_Screen

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multinav.chat.Message
import com.example.multinav.model.bluetooth_service.BluetoothService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JoyStickViewModel(
    private val bluetoothService: BluetoothService,
    private val deviceAddress: String,
    private val isMobileDevice: Boolean // Add isMobileDevice
) : ViewModel() {
    val selectedMode = mutableStateOf("")
    val isToggleButtonA = mutableStateOf(false)
    val isToggleButtonB = mutableStateOf(false)
    val isToggleButtonC = mutableStateOf(false)
    val isToggleButtonD = mutableStateOf(false)
    val currentAngle = mutableStateOf(0f)
    private val BUTTON_A_PRESS = "fd"   // ASCII 'A'
    private val BUTTON_A_RELEASE = "ol"  // ASCII 'a'

    // Add sensor reading states
    private val _temperature = MutableStateFlow("-")
    val temperature: StateFlow<String> = _temperature

    private val _humidity = MutableStateFlow("-")
    val humidity: StateFlow<String> = _humidity

    private val _pressure = MutableStateFlow("-")
    val pressure: StateFlow<String> = _pressure

    private val _airQuality = MutableStateFlow("-")
    val airQuality: StateFlow<String> = _airQuality

    init {
        if (!bluetoothService.isConnected.value) {
            reconnect()
        }
        setupSensorDataListener()

    }

    private fun setupSensorDataListener() {
        // Listen for messages from the device
        viewModelScope.launch {
            bluetoothService.messagesFlow.collect { messagesMap ->
                // Get messages for our connected device
                val deviceMessages = messagesMap[deviceAddress] ?: return@collect

                // Process the latest message
                val latestMessage = deviceMessages.lastOrNull() ?: return@collect

                if (latestMessage is Message.Text) {
                    processSensorData(latestMessage.text)
                }
            }
        }
    }

    private fun processSensorData(message: String) {
        Log.d("JoyStickViewModel", "Processing message: $message")

        // Check if the message contains sensor data
        if (message.startsWith("SENSOR:")) {
            val sensorData = message.substringAfter("SENSOR:")

            // Parse sensor data - expected format: "TEMP=24.5;HUM=48.2;PRESS=1013.2;AQ=Good"
            sensorData.split(";").forEach { dataPart ->
                val parts = dataPart.split("=")
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()

                    when (key) {
                        "TEMP" -> _temperature.value = value
                        "HUM" -> _humidity.value = value
                        "PRESS" -> _pressure.value = value
                        "AQ" -> _airQuality.value = value
                    }
                }
            }

            Log.d("JoyStickViewModel", "Updated sensor data: T=${_temperature.value}, H=${_humidity.value}, P=${_pressure.value}, AQ=${_airQuality.value}")
        }
    }

    private fun reconnect() {
        viewModelScope.launch {
            try {
                Log.d("JoyStickViewModel", "Attempting to reconnect to device: $deviceAddress, isMobileDevice: $isMobileDevice")
                val success = bluetoothService.connectToDevice(deviceAddress) // Use isMobileDevice
                if (success) {
                    Log.d("JoyStickViewModel", "Reconnected successfully")
                } else {
                    Log.e("JoyStickViewModel", "Failed to reconnect to device")
                }
            } catch (e: Exception) {
                Log.e("JoyStickViewModel", "Error reconnecting: ${e.message}", e)
            }
        }
    }

    fun onButtonAClick(isPressed: Boolean) {
        isToggleButtonA.value = isPressed
        viewModelScope.launch {
            val command = if (isPressed) BUTTON_A_PRESS else BUTTON_A_RELEASE
            Log.d("JoyStickViewModel", "Attempting to send command: $command, isConnected: ${bluetoothService.isConnected.value}")
            try {
                if (!bluetoothService.isConnected.value) {
                    Log.d("JoyStickViewModel", "Not connected, attempting to reconnect")
                    reconnect()
                    if (!bluetoothService.isConnected.value) {
                        Log.e("JoyStickViewModel", "Still not connected after reconnect attempt")
                        return@launch
                    }
                }
                bluetoothService.sendTextMessage(command) // Use isMobileDevice
                Log.d("JoyStick", "Sent command: $command")
            } catch (e: Exception) {
                Log.e("JoyStick", "Error sending command: ${e.message}", e)
            }
        }
    }



    fun sendActionCommand(s: String) {

    }

    fun sendDirectionCommand(s: String) {

    }

    fun onButtonBClick(it: Boolean) {

    }

    fun onButtonCClick(it: Boolean) {

    }

    fun onButtonDClick(it: Boolean) {

    }
}

class JoyStickViewModelFactory(
    private val bluetoothService: BluetoothService,
    private val deviceAddress: String,
    private val isMobileDevice: Boolean // Add isMobileDevice
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JoyStickViewModel::class.java)) {
            return JoyStickViewModel(bluetoothService, deviceAddress, isMobileDevice) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}