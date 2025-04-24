package com.example.multinav

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
        private val _state = MutableStateFlow(BluetoothUiState())
        val state = _state.asStateFlow()

        init {
            updatePairedDevices()
            startBluetoothOperations()
        }

        fun startBluetoothOperations() {
            viewModelScope.launch {
                try {
                    bluetoothService.startAdvertising()
                    startScanning()
                } catch (e: Exception) {
                    Log.e("BluetoothViewModel", "Failed to start BLE operations", e)
                    _state.update { it.copy(errorMessage = "Failed to start BLE: ${e.message}") }
                }
            }
        }

        private fun startScanning() {
            viewModelScope.launch {
                _state.update { it.copy(isScanning = true) }
                try {
                    bluetoothService.startAdvertising()
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = "Scanning failed: ${e.message}"
                        )
                    }
                }
            }
        }

        fun openBluetoothSettings() {
            bluetoothService.openBluetoothSettings()
        }

        private fun updatePairedDevices() {
            _state.update {
                it.copy(pairedDevices = bluetoothService.getPairedDevices())
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
                    Log.e("BluetoothViewModel", "Connection error", e)
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

        fun connectToDeviceAndNavigate(
            device: BluetoothDeviceData,
            onNavigate: () -> Unit
        ) {
            viewModelScope.launch {
                try {
                    val success = bluetoothService.connectToDevice(device.address)
                    if (success) {
                        _state.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = true,
                                errorMessage = null
                            )
                        }
                        onNavigate()
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
                    Log.e("BluetoothViewModel", "Connection error", e)
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
                try {
                    if (!bluetoothService.sendMessage(message)) {
                        _state.update {
                            it.copy(errorMessage = "Failed to send message")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BluetoothViewModel", "Send message error", e)
                    _state.update {
                        it.copy(errorMessage = "Send error: ${e.message}")
                    }
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            deviceConnectionJob?.cancel()
            bluetoothService.disconnect()
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