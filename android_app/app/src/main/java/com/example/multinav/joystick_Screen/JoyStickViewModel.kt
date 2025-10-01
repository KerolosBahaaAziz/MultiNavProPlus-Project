package com.example.joystick_Screen

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multinav.chat.Message
import com.example.multinav.model.bluetooth_service.BluetoothService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JoyStickViewModel(
    private val bluetoothService: BluetoothService,
    private val deviceAddress: String,
    private val isMobileDevice: Boolean
) : ViewModel() {
    val selectedMode = mutableStateOf("")
    val isToggleButtonA = mutableStateOf(false)
    val isToggleButtonB = mutableStateOf(false)
    val isToggleButtonC = mutableStateOf(false)
    val isToggleButtonD = mutableStateOf(false)
    val currentAngle = mutableStateOf(0f)
    private val BUTTON_A_PRESS = "fd"
    private val BUTTON_A_RELEASE = "ol"

    // Add sensor reading states
    private val _temperature = MutableStateFlow("-")
    val temperature: StateFlow<String> = _temperature

    private val _humidity = MutableStateFlow("-")
    val humidity: StateFlow<String> = _humidity

    private val _pressure = MutableStateFlow("-")
    val pressure: StateFlow<String> = _pressure

    private val _airQuality = MutableStateFlow("-")
    val airQuality: StateFlow<String> = _airQuality

    // Add GNSS amplitude state
    private val _gnssAmplitude = MutableStateFlow("-")
    val gnssAmplitude: StateFlow<String> = _gnssAmplitude

    // Track last update time for each sensor
    private val sensorLastUpdateTime = mutableMapOf(
        "TEMP" to 0L,
        "HUM" to 0L,
        "PRESS" to 0L,
        "AQ" to 0L,
        "GNSS" to 0L
    )

    // Timeout duration in milliseconds (5 seconds)
    private val SENSOR_TIMEOUT = 5000L

    init {
        // Try reconnect if not connected
        if (!bluetoothService.isConnected.value) {
            reconnect()
        }

        // Listen for connection state changes
        viewModelScope.launch {
            bluetoothService.isConnected.collect { connected ->
                if (!connected) {
                    resetSensorData()
                }
            }
        }

        // Listen for sensor data messages
        setupSensorDataListener()

        // Start sensor timeout monitor
        startSensorTimeoutMonitor()
    }

    private fun startSensorTimeoutMonitor() {
        viewModelScope.launch {
            while (true) {
                delay(1000) // Check every second

                val currentTime = System.currentTimeMillis()

                // Check each sensor for timeout
                sensorLastUpdateTime.forEach { (sensor, lastUpdate) ->
                    if (lastUpdate > 0 && (currentTime - lastUpdate) > SENSOR_TIMEOUT) {
                        // Sensor data is stale, reset to "-"
                        when (sensor) {
                            "TEMP" -> {
                                if (_temperature.value != "-") {
                                    _temperature.value = "-"
                                    Log.d("JoyStickViewModel", "Temperature sensor timeout")
                                }
                            }
                            "HUM" -> {
                                if (_humidity.value != "-") {
                                    _humidity.value = "-"
                                    Log.d("JoyStickViewModel", "Humidity sensor timeout")
                                }
                            }
                            "PRESS" -> {
                                if (_pressure.value != "-") {
                                    _pressure.value = "-"
                                    Log.d("JoyStickViewModel", "Pressure sensor timeout")
                                }
                            }
                            "AQ" -> {
                                if (_airQuality.value != "-") {
                                    _airQuality.value = "-"
                                    Log.d("JoyStickViewModel", "Air quality sensor timeout")
                                }
                            }
                            "GNSS" -> {
                                if (_gnssAmplitude.value != "-") {
                                    _gnssAmplitude.value = "-"
                                    Log.d("JoyStickViewModel", "GNSS sensor timeout")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun resetSensorData() {
        _temperature.value = "-"
        _humidity.value = "-"
        _pressure.value = "-"
        _airQuality.value = "-"
        _gnssAmplitude.value = "-"

        // Reset timestamps
        sensorLastUpdateTime.keys.forEach { key ->
            sensorLastUpdateTime[key] = 0L
        }
    }

    private fun setupSensorDataListener() {
        viewModelScope.launch {
            bluetoothService.messagesFlow.collect { messagesMap ->
                val deviceMessages = messagesMap[deviceAddress] ?: return@collect
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
            val currentTime = System.currentTimeMillis()

            // Parse sensor data - expected format: "TEMP=24.5;HUM=48.2;PRESS=1013.2;AQ=Good;GNSS=45.3"
            sensorData.split(";").forEach { dataPart ->
                val parts = dataPart.split("=")
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()

                    when (key) {
                        "TEMP" -> {
                            _temperature.value = value
                            sensorLastUpdateTime["TEMP"] = currentTime
                        }
                        "HUM" -> {
                            _humidity.value = value
                            sensorLastUpdateTime["HUM"] = currentTime
                        }
                        "PRESS" -> {
                            _pressure.value = value
                            sensorLastUpdateTime["PRESS"] = currentTime
                        }
                        "AQ" -> {
                            _airQuality.value = value
                            sensorLastUpdateTime["AQ"] = currentTime
                        }
                        "GNSS" -> {
                            _gnssAmplitude.value = value
                            sensorLastUpdateTime["GNSS"] = currentTime
                        }
                    }
                }
            }

            Log.d("JoyStickViewModel", "Updated sensor data: T=${_temperature.value}, H=${_humidity.value}, P=${_pressure.value}, AQ=${_airQuality.value}, GNSS=${_gnssAmplitude.value}")
        }
    }

    private fun reconnect() {
        viewModelScope.launch {
            try {
                Log.d("JoyStickViewModel", "Attempting to reconnect to device: $deviceAddress, isMobileDevice: $isMobileDevice")
                val success = bluetoothService.connectToDevice(deviceAddress)
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
                bluetoothService.sendTextMessage(command)
                Log.d("JoyStick", "Sent command: $command")
            } catch (e: Exception) {
                Log.e("JoyStick", "Error sending command: ${e.message}", e)
            }
        }
    }

    fun sendActionCommand(s: String) {
        // TODO: Implement action command
    }

    fun sendDirectionCommand(s: String) {
        // TODO: Implement direction command
    }

    fun onButtonBClick(it: Boolean) {
        // TODO: Implement button B click
    }

    fun onButtonCClick(it: Boolean) {
        // TODO: Implement button C click
    }

    fun onButtonDClick(it: Boolean) {
        // TODO: Implement button D click
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up resources if needed
    }
}

class JoyStickViewModelFactory(
    private val bluetoothService: BluetoothService,
    private val deviceAddress: String,
    private val isMobileDevice: Boolean
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JoyStickViewModel::class.java)) {
            return JoyStickViewModel(bluetoothService, deviceAddress, isMobileDevice) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}