package com.example.joystick_Screen

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multinav.BluetoothService
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

    init {
        if (!bluetoothService.isConnected.value) {
            reconnect()
        }
    }

    private fun reconnect() {
        viewModelScope.launch {
            try {
                Log.d("JoyStickViewModel", "Attempting to reconnect to device: $deviceAddress, isMobileDevice: $isMobileDevice")
                val success = bluetoothService.connectToDevice(deviceAddress, isMobileDevice) // Use isMobileDevice
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
                bluetoothService.sendMessage(command, isMobileDevice = isMobileDevice) // Use isMobileDevice
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