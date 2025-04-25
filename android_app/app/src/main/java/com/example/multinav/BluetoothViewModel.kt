package com.example.multinav

    import android.annotation.SuppressLint
    import android.util.Log
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.viewModelScope
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.TimeoutCancellationException
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.flow.update
    import kotlinx.coroutines.isActive
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withTimeout
    import kotlin.coroutines.cancellation.CancellationException

class BluetoothViewModel(
    val bluetoothService: BluetoothService
) : ViewModel() {
    private var deviceConnectionJob: Job? = null
    private var refreshJob: Job? = null
    private val _state = MutableStateFlow(BluetoothUiState())
    val state = _state.asStateFlow()

    init {
        updatePairedDevices()

        // Monitor connection status
        viewModelScope.launch {
            bluetoothService.connectionStatus.collect { status ->
                _state.update {
                    it.copy(
                        isConnecting = status is BluetoothService.ConnectionStatus.Connecting,
                        isConnected = status is BluetoothService.ConnectionStatus.Connected,
                        errorMessage = if (status is BluetoothService.ConnectionStatus.Error)
                            status.message else null
                    )
                }

                // Update device list when connection status changes
                if (status is BluetoothService.ConnectionStatus.Connected ||
                    status is BluetoothService.ConnectionStatus.Disconnected) {
                    updatePairedDevices()
                }
            }
        }
    }

    fun startServer() {
        viewModelScope.launch {
            try {
                stopPeriodicRefresh()
                bluetoothService.stopScanning()

                // Add a small delay before starting advertising
                delay(200)

                bluetoothService.startAdvertising()
                _state.update { it.copy(
                    isScanning = false,
                    errorMessage = null
                )}
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Failed to start server", e)
                _state.update { it.copy(errorMessage = "Failed to start server: ${e.message}") }
            }
        }
    }

    fun startClient() {
        viewModelScope.launch {
            try {
                bluetoothService.stopAdvertising()
                _state.update { it.copy(
                    errorMessage = null
                )}
                startScanning()
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Failed to start client", e)
                _state.update { it.copy(errorMessage = "Failed to start client: ${e.message}") }
            }
        }
    }

//    private fun startScanning() {
//        viewModelScope.launch {
//            _state.update { it.copy(isScanning = true) }
//            try {
//                bluetoothService.startScanning()
//                startPeriodicRefresh()
//            } catch (e: Exception) {
//                stopPeriodicRefresh()
//                _state.update {
//                    it.copy(
//                        isScanning = false,
//                        errorMessage = "Scanning failed: ${e.message}"
//                    )
//                }
//            }
//        }
//    }
@SuppressLint("MissingPermission")
fun startScanning() {
    viewModelScope.launch {
        bluetoothService.startLeScan { device ->
            _state.update { state ->
                val newDevice = BluetoothDeviceData(
                    name = device.name ?: "Unknownnnn",
                    address = device.address,
                    isConnected = false
                )
                state.copy(
                    scannedDevices = state.scannedDevices + newDevice
                )
            }
        }
    }
}

    fun stopScanning() {
        viewModelScope.launch {
            stopPeriodicRefresh()
            bluetoothService.stopScanning()
            _state.update { it.copy(isScanning = false) }
        }
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive && _state.value.isScanning) {
                updatePairedDevices()
                delay(2000) // Refresh every 2 seconds
            }
        }
    }

    private fun stopPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun openBluetoothSettings() {
        bluetoothService.openBluetoothSettings()
    }

    private fun updatePairedDevices() {
        _state.update {
            it.copy(pairedDevices = bluetoothService.getPairedDevices())
        }
    }

    fun connectToDeviceAndNavigate(
        device: BluetoothDeviceData,
        onNavigate: () -> Unit
    ) {
        // Cancel any existing connection job
        deviceConnectionJob?.cancel()

        // Create a new connection job with error handling
        deviceConnectionJob = viewModelScope.launch {
            try {
                _state.update { it.copy(isConnecting = true) }

                // Use a timeout to prevent hanging
                withTimeout(10000) { // 10 seconds timeout
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
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("BluetoothViewModel", "Connection timeout", e)
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = "Connection timeout"
                    )
                }
            } catch (e: CancellationException) {
                // Handle cancellation gracefully
                Log.d("BluetoothViewModel", "Connection cancelled", e)
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = null // Don't show error for cancellation
                    )
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

    fun disconnect() {
        viewModelScope.launch {
            bluetoothService.disconnect()
            _state.update {
                it.copy(
                    isConnected = false,
                    isConnecting = false
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        deviceConnectionJob?.cancel()
        refreshJob?.cancel()
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