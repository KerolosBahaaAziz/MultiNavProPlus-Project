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

    // Add GNSS location states
    private val _gnssLatitude = MutableStateFlow<Double?>(null)
    val gnssLatitude: StateFlow<Double?> = _gnssLatitude

    private val _gnssLongitude = MutableStateFlow<Double?>(null)
    val gnssLongitude: StateFlow<Double?> = _gnssLongitude


    // Track last update time for each sensor
    private val sensorLastUpdateTime = mutableMapOf(
        "TEMP" to 0L,
        "HUM" to 0L,
        "PRESS" to 0L,
        "AQ" to 0L,
        "GNSS_AMPLITUDE" to 0L,
        "GNSS_LAT" to 0L,
        "GNSS_LON" to 0L
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
                            "GNSS_AMPLITUDE" -> {
                                if (_gnssAmplitude.value != "-") {
                                    _gnssAmplitude.value = "-"
                                    Log.d("JoyStickViewModel", "GNSS amplitude timeout")
                                }
                            }
                            "GNSS_LAT" -> {
                                if (_gnssLatitude.value != null) {
                                    _gnssLatitude.value = null
                                    Log.d("JoyStickViewModel", "GNSS latitude timeout")
                                }
                            }
                            "GNSS_LON" -> {
                                if (_gnssLongitude.value != null) {
                                    _gnssLongitude.value = null
                                    Log.d("JoyStickViewModel", "GNSS longitude timeout")
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
            _gnssLatitude.value = null
            _gnssLongitude.value = null

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
        Log.d("JoyStickViewModel", "Processing raw message: '$message'")

        // Try different message formats
        when {
            message.startsWith("SENSOR:") -> {
                Log.d("JoyStickViewModel", "Found SENSOR: prefix")
                parseSensorData(message.substringAfter("SENSOR:"))
            }
            message.contains("TEMP=") || message.contains("HUM=") ||
                    message.contains("PRESS=") || message.contains("AQ=") ||
                    message.contains("GNSS") -> {
                Log.d("JoyStickViewModel", "Found sensor data without SENSOR: prefix")
                parseSensorData(message)
            }
            else -> {
                Log.d("JoyStickViewModel", "Message doesn't match sensor format: $message")
            }
        }
    }

    private fun parseSensorData(sensorData: String) {
        val currentTime = System.currentTimeMillis()

        // Parse sensor data - expected format: "TEMP=24.5;HUM=48.2;PRESS=1013.2;AQ=Good;GNSS_AMPLITUDE=45.3;GNSS_LAT=30.0444;GNSS_LON=31.2357"
        sensorData.split(";").forEach { dataPart ->
            val parts = dataPart.split("=")
            if (parts.size == 2) {
                val key = parts[0].trim().uppercase()
                val value = parts[1].trim()

                Log.d("JoyStickViewModel", "Parsing: $key = $value")

                when (key) {
                    "TEMP", "TEMPERATURE" -> {
                        _temperature.value = value
                        sensorLastUpdateTime["TEMP"] = currentTime
                        Log.d("JoyStickViewModel", "Updated temperature: $value")
                    }
                    "HUM", "HUMIDITY" -> {
                        _humidity.value = value
                        sensorLastUpdateTime["HUM"] = currentTime
                        Log.d("JoyStickViewModel", "Updated humidity: $value")
                    }
                    "PRESS", "PRESSURE" -> {
                        _pressure.value = value
                        sensorLastUpdateTime["PRESS"] = currentTime
                        Log.d("JoyStickViewModel", "Updated pressure: $value")
                    }
                    "AQ", "AIRQUALITY", "AIR_QUALITY" -> {
                        _airQuality.value = value
                        sensorLastUpdateTime["AQ"] = currentTime
                        Log.d("JoyStickViewModel", "Updated air quality: $value")
                    }
                    "GNSS_AMPLITUDE", "GNSS_AMP", "GNSS" -> {
                        _gnssAmplitude.value = value
                        sensorLastUpdateTime["GNSS_AMPLITUDE"] = currentTime
                        Log.d("JoyStickViewModel", "Updated GNSS amplitude: $value")
                    }
                    "GNSS_LAT", "GNSS_LATITUDE" -> {
                        val latitude = value.toDoubleOrNull()
                        if (latitude != null && latitude >= -90 && latitude <= 90) {
                            _gnssLatitude.value = latitude
                            sensorLastUpdateTime["GNSS_LAT"] = currentTime
                            Log.d("JoyStickViewModel", "Updated GNSS latitude: $latitude")
                        } else {
                            Log.e("JoyStickViewModel", "Invalid GNSS latitude value: $value")
                        }
                    }
                    "GNSS_LON", "GNSS_LONG", "GNSS_LONGITUDE" -> {
                        val longitude = value.toDoubleOrNull()
                        if (longitude != null && longitude >= -180 && longitude <= 180) {
                            _gnssLongitude.value = longitude
                            sensorLastUpdateTime["GNSS_LON"] = currentTime
                            Log.d("JoyStickViewModel", "Updated GNSS longitude: $longitude")
                        } else {
                            Log.e("JoyStickViewModel", "Invalid GNSS longitude value: $value")
                        }
                    }
                    else -> {
                        Log.w("JoyStickViewModel", "Unknown sensor key: $key")
                    }
                }
            } else {
                Log.w("JoyStickViewModel", "Invalid sensor data format: $dataPart")
            }
        }

        Log.d("JoyStickViewModel",
            "Final sensor values - T:${_temperature.value}, H:${_humidity.value}, " +
                    "P:${_pressure.value}, AQ:${_airQuality.value}, " +
                    "GNSS Amp:${_gnssAmplitude.value}, " +
                    "GNSS Lat:${_gnssLatitude.value}, GNSS Lon:${_gnssLongitude.value}"
        )
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