package com.example.multinav.bluetooth

    import android.annotation.SuppressLint
    import android.util.Log
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.viewModelScope
    import com.example.multinav.BluetoothDeviceData
    import com.example.multinav.BluetoothService
    import com.example.multinav.BluetoothUiState
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.TimeoutCancellationException
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.flow.collectLatest
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
    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState

    init {
        // Monitor Bluetooth state and fetch paired devices when enabled
        viewModelScope.launch {
            bluetoothService.bluetoothState.collectLatest { isEnabled ->
                Log.d("BluetoothViewModel", "Bluetooth state changed: $isEnabled")
                _uiState.value = _uiState.value.copy(isBluetoothEnabled = isEnabled)
                if (isEnabled) {
                    fetchPairedDevices()
                } else {
                    _uiState.value = _uiState.value.copy(
                        pairedDevices = emptyList(),
                        errorMessage = "Bluetooth is disabled"
                    )
                    bluetoothService.enableBluetooth()
                }
            }
        }
    }

    private fun fetchPairedDevices() {
        viewModelScope.launch {
            try {
                val pairedDevices = bluetoothService.getPairedDevices()
                _uiState.value = _uiState.value.copy(
                    pairedDevices = pairedDevices,
                    errorMessage = if (pairedDevices.isEmpty()) "No paired devices found" else null
                )
                Log.d("BluetoothViewModel", "Fetched paired devices: $pairedDevices")
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Error fetching paired devices", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error fetching paired devices: ${e.message}"
                )
            }
        }
    }

    fun refreshDevices() {
        viewModelScope.launch {
            if (bluetoothService.isBluetoothEnabled()) {
                fetchPairedDevices()
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Bluetooth is disabled. Please enable it to refresh devices."
                )
                bluetoothService.enableBluetooth()
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
                _uiState.update { it.copy(
                    isScanning = false,
                    errorMessage = null
                )}
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Failed to start server", e)
                _uiState.update { it.copy(errorMessage = "Failed to start server: ${e.message}") }
            }
        }
    }

    fun startClient() {
        viewModelScope.launch {
            try {
                bluetoothService.stopAdvertising()
                _uiState.update { it.copy(
                    errorMessage = null
                )}
                startScanning()
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Failed to start client", e)
                _uiState.update { it.copy(errorMessage = "Failed to start client: ${e.message}") }
            }
        }
    }


    @SuppressLint("MissingPermission")
fun startScanning() {
    viewModelScope.launch {
        try {
            Log.d("BluetoothViewModel", "Starting BLE scan...")
            bluetoothService.stopScanning()
            Log.d("BluetoothViewModel", "Stopped previous scan")
            _uiState.update {
                Log.d("BluetoothViewModel", "Updating UI state to scanning")
                it.copy(
                    isScanning = true,
                    scannedDevices = emptyList(),
                    errorMessage = null
                )
            }
            bluetoothService.startLeScan { device, rssi, isMobileDevice ->
                Log.d("BluetoothViewModel", "Device found - Name: ${device.name ?: "Unknown"}, Address: ${device.address}, RSSI: $rssi, IsMobile: $isMobileDevice")
                _uiState.update { state ->
                    val newDevice = BluetoothDeviceData(
                        name = device.name ?: "Unknown Device",
                        address = device.address,
                        rssi = rssi,
                        isConnected = false,
                        isMobileDevice = isMobileDevice
                    )
                    if (!state.scannedDevices.any { it.address == newDevice.address }) {
                        Log.d("BluetoothViewModel", "Adding new device to list: ${newDevice.name}")
                        state.copy(scannedDevices = state.scannedDevices + newDevice)
                    } else {
                        Log.d("BluetoothViewModel", "Device already in list: ${newDevice.name}")
                        state
                    }
                }
            }
            Log.d("BluetoothViewModel", "Scan started, waiting 15 seconds...")
            delay(30000)
            Log.d("BluetoothViewModel", "15 seconds elapsed, stopping scan")
            stopScanning()
        } catch (e: Exception) {
            Log.e("BluetoothViewModel", "Scan failed with error: ${e.message}", e)
            _uiState.update {
                it.copy(
                    isScanning = false,
                    errorMessage = "Scan failed: ${e.message}"
                )
            }
        }
    }
}
    fun stopScanning() {
        viewModelScope.launch {
            stopPeriodicRefresh()
            bluetoothService.stopScanning()
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive && _uiState.value.isScanning) {
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
        _uiState.update {
            it.copy(pairedDevices = bluetoothService.getPairedDevices())
        }
    }

    fun connectToDeviceAndNavigate(
        device: BluetoothDeviceData,
        onNavigate: () -> Unit,
        isFromPairedList: Boolean // New parameter to indicate if the device is from paired list
    ) {
        // Cancel any existing connection job
        deviceConnectionJob?.cancel()

        // Determine isMobileDevice based on whether the device is from paired list or scanned list
        if (isFromPairedList) {
            bluetoothService.isMobileDevice=true

        } else {
            // For scanned devices, check the name for BLE patterns
            val deviceName = device.name ?: "Unknown"
            val isBleDevice = deviceName.contains("BLE", ignoreCase = true) || deviceName == "BLE_WB07"
            if (isBleDevice) {
                bluetoothService.isMobileDevice=false

            } else {
                bluetoothService.isMobileDevice=true
            }
        }

        Log.d("BluetoothViewModel", "Connecting to device: ${device.name}, Address: ${device.address}")

        // Create a new connection job with error handling
        deviceConnectionJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isConnecting = true) }

                // Use a timeout to prevent hanging
                withTimeout(10000) { // 10 seconds timeout
                    val success = bluetoothService.connectToDevice(device.address, bluetoothService.isMobileDevice)
                    if (success) {
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = true,
                                errorMessage = null
                            )
                        }
                        onNavigate()
                    } else {
                        _uiState.update {
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
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = "Connection timeout"
                    )
                }
            } catch (e: CancellationException) {
                // Handle cancellation gracefully
                Log.d("BluetoothViewModel", "Connection cancelled", e)
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = null // Don't show error for cancellation
                    )
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Connection error", e)
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

//    fun connectToDeviceAndNavigate(
//        device: BluetoothDeviceData,
//        onNavigate: () -> Unit
//    ) {
//        // Cancel any existing connection job
//        deviceConnectionJob?.cancel()
//
//        // Create a new connection job with error handling
//        deviceConnectionJob = viewModelScope.launch {
//            try {
//                _uiState.update { it.copy(isConnecting = true) }
//
//                // Use a timeout to prevent hanging
//                withTimeout(10000) { // 10 seconds timeout
//                    val success = bluetoothService.connectToDevice(device.address, device.isMobileDevice)
//                    if (success) {
//                        _uiState.update {
//                            it.copy(
//                                isConnecting = false,
//                                isConnected = true,
//                                errorMessage = null
//                            )
//                        }
//                        onNavigate()
//                    } else {
//                        _uiState.update {
//                            it.copy(
//                                isConnecting = false,
//                                isConnected = false,
//                                errorMessage = "Failed to connect"
//                            )
//                        }
//                    }
//                }
//            } catch (e: TimeoutCancellationException) {
//                Log.e("BluetoothViewModel", "Connection timeout", e)
//                _uiState.update {
//                    it.copy(
//                        isConnecting = false,
//                        isConnected = false,
//                        errorMessage = "Connection timeout"
//                    )
//                }
//            } catch (e: CancellationException) {
//                // Handle cancellation gracefully
//                Log.d("BluetoothViewModel", "Connection cancelled", e)
//                _uiState.update {
//                    it.copy(
//                        isConnecting = false,
//                        isConnected = false,
//                        errorMessage = null // Don't show error for cancellation
//                    )
//                }
//            } catch (e: Exception) {
//                Log.e("BluetoothViewModel", "Connection error", e)
//                _uiState.update {
//                    it.copy(
//                        isConnecting = false,
//                        isConnected = false,
//                        errorMessage = e.message
//                    )
//                }
//            }
//        }
//    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothService.disconnect()
            _uiState.update {
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