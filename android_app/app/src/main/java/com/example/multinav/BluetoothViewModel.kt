
package com.example.multinav

import com.example.multinav.BluetoothDeviceData


    import android.content.ContentValues.TAG
    import android.util.Log
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.viewModelScope
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.flow.update
    import kotlinx.coroutines.launch

class BluetoothViewModel(
    private val bluetoothService: BluetoothService
): ViewModel() {
    private var deviceConnectionJob: Job? = null
    private val TAG = "Bluetooth"

    private val _state = MutableStateFlow(BluetoothUiState())
    val state = _state.asStateFlow()


    init {
        updatePairedDevices()
    }

    fun openBluetoothSettings() {
        bluetoothService.openBluetoothSettings()
    }

    private fun updatePairedDevices() {
        _state.update {
            it.copy(
                pairedDevices = bluetoothService.getPairedDevices()
            )
        }
    }

    fun connectToDevice(device: BluetoothDeviceData, onSuccess: () -> Unit) {
        deviceConnectionJob?.cancel()
        deviceConnectionJob = viewModelScope.launch {
            _state.update { it.copy(isConnecting = true) }
            try {
                if (bluetoothService.connectToDevice(device.address)) {
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = true,
                            errorMessage = null
                        )
                    }
                    onSuccess()
                } else {
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            errorMessage = "Failed to connect"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }


    // Add this method to navigate immediately and connect in background
    fun connectToDeviceAndNavigate(
        device: BluetoothDeviceData,
        onNavigate: () -> Unit) {
        // Navigate immediately
        viewModelScope.launch {
            val success = bluetoothService.connectToDevice(device.address)
            if (success) {
                onNavigate()
            }
        }

        // Then attempt connection in the background
        deviceConnectionJob?.cancel()
        deviceConnectionJob = viewModelScope.launch {
            _state.update { it.copy(isConnecting = true) }
            try {
                if (bluetoothService.connectToDevice(device.address)) {
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = true,
                            errorMessage = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            errorMessage = "Failed to connect"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }


    fun sendMessage(message: String) {
        viewModelScope.launch {
            if (!bluetoothService.sendMessage(message)) {
                _state.update {
                    it.copy(
                        errorMessage = "Failed to send message"
                    )
                }
            }
        }
    }
}

    class BluetoothViewModelFactory(
        private val bluetoothService: BluetoothService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
                return BluetoothViewModel(bluetoothService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
