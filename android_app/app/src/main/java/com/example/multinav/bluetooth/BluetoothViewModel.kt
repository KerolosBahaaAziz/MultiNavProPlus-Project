package com.example.multinav.bluetooth

    import android.annotation.SuppressLint
    import android.os.ParcelUuid
    import android.util.Log
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.viewModelScope
    import com.example.multinav.BLEConfig
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

//    private fun startScanning() {
//        viewModelScope.launch {
//            _uiState.update { it.copy(isScanning = true) }
//            try {
//                bluetoothService.startScanning()
//                startPeriodicRefresh()
//            } catch (e: Exception) {
//                stopPeriodicRefresh()
//                _uiState.update {
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
            bluetoothService.startLeScan { device, rssi ,result ->
                // Determine if the device is a mobile device by checking advertised UUIDs
                // Note: This requires access to the scan record, which may need to be provided by BluetoothService
                val isMobileDevice = result.scanRecord?.getServiceUuids()?.contains(ParcelUuid(BLEConfig.CHAT_SERVICE_UUID)) == true
                Log.d("BLE", "Device found - Name: ${device.name ?: "Unknown"}, Address: ${device.address}, RSSI: $rssi, IsMobile: $isMobileDevice")
                _uiState.update { state ->
                    val newDevice = BluetoothDeviceData(
                        name = device.name ?: "Unknown Device",
                        address = device.address,
                        isConnected = false,
                        rssi = rssi,
                        isMobileDevice = isMobileDevice // Set based on advertised UUID
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
            delay(15000)
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
        onNavigate: () -> Unit
    ) {
        // Cancel any existing connection job
        deviceConnectionJob?.cancel()

        // Create a new connection job with error handling
        deviceConnectionJob = viewModelScope.launch {
            try {
                stopScanning()
                val success = bluetoothService.connectToDevice(device.address, device.isMobileDevice)
                if (success) {
                    onNavigate()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to connect to ${device.name ?: "device"}"
                    )
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Error connecting to device", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error connecting to ${device.name ?: "device"}: ${e.message}"
                )
            }
        }
    }

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