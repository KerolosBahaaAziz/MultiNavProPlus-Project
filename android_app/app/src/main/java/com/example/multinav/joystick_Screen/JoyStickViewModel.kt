package com.example.joystick_Screen

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.multinav.BluetoothService
import kotlinx.coroutines.launch

class JoyStickViewModel (
    private val bluetoothService: BluetoothService
): ViewModel() {
    val selectedMode =  mutableStateOf("")
    val isToggleButtonA = mutableStateOf(false)
    val isToggleButtonB = mutableStateOf(false)
    val isToggleButtonC = mutableStateOf(false)
    val isToggleButtonD = mutableStateOf(false)
    val currentAngle = mutableStateOf(0f)
    private val BUTTON_A_PRESS = "0x41"   // ASCII 'A'
    private val BUTTON_A_RELEASE = "0x61"  // ASCII 'a

    fun onButtonAClick(isPressed: Boolean) {
        isToggleButtonA.value = isPressed
        viewModelScope.launch {
            val command = if (isPressed) BUTTON_A_PRESS else BUTTON_A_RELEASE
            try {
                bluetoothService.sendMessage("f", isMobileDevice = false)
                Log.d("JoyStick", "Sent command: $command")
            } catch (e: Exception) {
                Log.e("JoyStick", "Error sending command", e)
            }
        }
    }
}

class JoyStickViewModelFactory(
    private val bluetoothService: BluetoothService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JoyStickViewModel::class.java)) {
            return JoyStickViewModel(bluetoothService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}