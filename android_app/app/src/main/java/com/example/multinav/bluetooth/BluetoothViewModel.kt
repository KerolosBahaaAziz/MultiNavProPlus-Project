package com.example.multinav.bluetooth

    import android.annotation.SuppressLint
    import android.util.Log
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.viewModelScope
    import com.example.multinav.model.bluetooth_service.BluetoothDeviceData
    import com.example.multinav.model.bluetooth_service.BluetoothService
    import com.example.multinav.model.bluetooth_service.BluetoothUiState
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.TimeoutCancellationException
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.collectLatest
    import kotlinx.coroutines.flow.takeWhile
    import kotlinx.coroutines.flow.update
    import kotlinx.coroutines.isActive
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withTimeout
    import kotlin.coroutines.cancellation.CancellationException

class BluetoothViewModel(
    val bluetoothService: BluetoothService
) : ViewModel() {
    private var deviceConnectionJob: Job? = null
    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState

    // Track BLE module connection state
    private val _bleModuleConnected = MutableStateFlow(false)
    val bleModuleConnected: StateFlow<Boolean> = _bleModuleConnected

    // Bottom sheet state
    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet: StateFlow<Boolean> = _showBottomSheet

    init {
        // Monitor Bluetooth state
        viewModelScope.launch {
            bluetoothService.bluetoothState.collectLatest { isEnabled ->
                Log.d("BluetoothViewModel", "Bluetooth state changed: $isEnabled")
                _uiState.update { it.copy(isBluetoothEnabled = isEnabled) }
                if (!isEnabled) {
                    _uiState.update { it.copy(errorMessage = "Bluetooth is disabled") }
                    bluetoothService.enableBluetooth()
                }
            }
        }

        // Monitor connection status
        viewModelScope.launch {
            bluetoothService.connectionStatus.collectLatest { status ->
                when (status) {
                    is BluetoothService.ConnectionStatus.Connected -> {
                        _bleModuleConnected.value = true
                        _uiState.update { it.copy(scannedDevices = it.scannedDevices.map { device ->
                            if (device.address == bluetoothService.getConnectedDeviceAddress()) {
                                device.copy(isConnected = true)
                            } else device
                        }) }
                    }
                    is BluetoothService.ConnectionStatus.Disconnected -> {
                        _bleModuleConnected.value = false
                        _uiState.update { it.copy(scannedDevices = it.scannedDevices.map { it.copy(isConnected = false) }) }
                    }
                    is BluetoothService.ConnectionStatus.Connecting -> {
                        _uiState.update { it.copy(isConnecting = true) }
                    }
                    is BluetoothService.ConnectionStatus.Error -> {
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                errorMessage = status.message
                            )
                        }
                    }
                }
            }
        }

        // Monitor devices from BLE module
        viewModelScope.launch {
            bluetoothService.scannedDevicesFromBle.collect { devices ->
                Log.d("BLE", "ViewModel received ${devices.size} devices from service")
                _uiState.update { state ->
                    state.copy(
                        scannedDevicesFromBle = devices,
                        statusMessage = if (devices.isNotEmpty())
                            "Found ${devices.size} devices"
                        else
                            state.statusMessage
                    )
                }
            }
        }

        // Monitor scanning state
        viewModelScope.launch {
            bluetoothService.isScanningState.collect { isScanning ->
                _uiState.update { state ->
                    state.copy(
                        isBleModuleScanning = isScanning,
                        statusMessage = when {
                            isScanning -> "Scanning for devices..."
                            !isScanning && state.scannedDevicesFromBle.isNotEmpty() ->
                                "Scan complete: Found ${state.scannedDevicesFromBle.size} devices"
                            !isScanning && state.scannedDevicesFromBle.isEmpty() ->
                                "No devices found"
                            else -> state.statusMessage
                        },
                        scanCompleted = !isScanning
                    )
                }
            }
        }

        // Monitor initial scanned devices
        viewModelScope.launch {
            bluetoothService.scannedDevices.collect { devices ->
                Log.d("BluetoothViewModel", "Initial scanned devices: ${devices.size}")
                _uiState.update { it.copy(scannedDevices = devices) }
            }
        }

        // Monitor initial scanning state
        viewModelScope.launch {
            bluetoothService.isInitialScanningState.collect { isScanning ->
                Log.d("BluetoothViewModel", "Initial scanning state: $isScanning")
                _uiState.update { it.copy(isScanning = isScanning) }
            }
        }
    }

    fun startInitialBleScan() {
        Log.d("BluetoothViewModel", "Starting initial BLE scan")
        _uiState.update {
            it.copy(
                isScanning = true,
                errorMessage = null,
                scannedDevices = emptyList()
            )
        }

        viewModelScope.launch {
            try {
                val success = bluetoothService.startInitialBleScan()
                if (!success) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = "Failed to start scan. Check permissions and location services."
                        )
                    }
                    return@launch
                }

                withTimeout(15000) {
                    bluetoothService.isInitialScanningState
                        .takeWhile { isScanning -> isScanning }
                        .collect { /* wait */ }
                }

                _uiState.update { it.copy(isScanning = false) }
            } catch (e: TimeoutCancellationException) {
                Log.e("BluetoothViewModel", "Initial scan timeout", e)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = "Initial scan timed out"
                    )
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Initial scan error", e)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = "Initial scan error: ${e.message}"
                    )
                }
            }
        }
    }

    fun showDeviceBottomSheet() {
        Log.d("BluetoothViewModel", "Showing bottom sheet")
        _showBottomSheet.value = true
    }

    fun hideDeviceBottomSheet() {
        _showBottomSheet.value = false
    }

    fun requestBleModuleScan() {
        Log.d("BLE", "Requesting BLE module scan")

        _uiState.update {
            it.copy(
                isBleModuleScanning = true,
                statusMessage = "Starting scan...",
                scanCompleted = false,
                scannedDevicesFromBle = emptyList()
            )
        }

        viewModelScope.launch {
            try {
                val success = bluetoothService.requestBleModuleScan()

                if (!success) {
                    Log.e("BLE", "BLE module scan request failed")
                    _uiState.update {
                        it.copy(
                            isBleModuleScanning = false,
                            errorMessage = "Failed to start scan. Ensure a BLE module is connected.",
                            statusMessage = "Scan failed to start",
                            scanCompleted = true
                        )
                    }
                    return@launch
                }

                // Wait for scan to complete with timeout
                withTimeout(35000) {
                    bluetoothService.isScanningState
                        .takeWhile { isScanning -> isScanning }
                        .collect { /* just wait */ }
                }

                // Scan completed
                _uiState.update {
                    it.copy(
                        isBleModuleScanning = false,
                        scanCompleted = true
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("BLE", "Scan timeout", e)
                _uiState.update {
                    it.copy(
                        isBleModuleScanning = false,
                        errorMessage = "Scan timed out",
                        statusMessage = "Scan timed out",
                        scanCompleted = true
                    )
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error requesting BLE module scan", e)
                _uiState.update {
                    it.copy(
                        isBleModuleScanning = false,
                        errorMessage = "Error: ${e.message}",
                        statusMessage = "Scan error occurred",
                        scanCompleted = true
                    )
                }
            }
        }
    }

    fun connectToDeviceByIndexAndNavigate(
        index: Int,
        onNavigate: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val devices = _uiState.value.scannedDevicesFromBle

                if (index < 0 || index >= devices.size) {
                    _uiState.update {
                        it.copy(errorMessage = "Invalid device selection")
                    }
                    return@launch
                }

                val selectedDevice = devices[index]
                Log.d("BLE", "Selected device at index $index: ${selectedDevice.name}")

                _uiState.update {
                    it.copy(
                        isConnecting = true,
                        errorMessage = null
                    )
                }

                val success = bluetoothService.connectToDeviceByIndex(index)

                if (success) {
                    Log.d("BLE", "Successfully connected to device at index $index")

                    _uiState.update { state ->
                        val updatedDevices = state.scannedDevicesFromBle.mapIndexed { i, device ->
                            if (i == index) device.copy(isConnected = true) else device
                        }
                        state.copy(
                            isConnecting = false,
                            errorMessage = null,
                            scannedDevicesFromBle = updatedDevices
                        )
                    }

                    bluetoothService.saveLastConnectedAddress(selectedDevice.address)
                    hideDeviceBottomSheet()
                    onNavigate(selectedDevice.address)
                } else {
                    Log.e("BLE", "Failed to connect to device at index $index")
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            errorMessage = "Failed to connect to selected device"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error connecting to device by index", e)
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        errorMessage = "Connection error: ${e.message}"
                    )
                }
            }
        }
    }

    fun connectToDevice(address: String, onNavigate: () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isConnecting = true, errorMessage = null) }

                withTimeout(10000) {
                    val success = bluetoothService.connectToDevice(address)

                    if (success) {
                        _uiState.update { it.copy(isConnecting = false) }

                        // Request scan and show bottom sheet
                        requestBleModuleScan()
                        showDeviceBottomSheet()
                    } else {
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
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
                        errorMessage = "Connection timeout"
                    )
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Connection error", e)
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun openBluetoothSettings() {
        bluetoothService.openBluetoothSettings()
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothService.disconnect()
            _bleModuleConnected.value = false
            hideDeviceBottomSheet()
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    scannedDevicesFromBle = emptyList()
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            Log.d("BluetoothViewModel", "ViewModel being cleared, disconnecting BLE")
            bluetoothService.disconnect()
        }
    }
}

class BluetoothViewModelFactory(
    private val bluetoothService: BluetoothService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BluetoothViewModel(bluetoothService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}