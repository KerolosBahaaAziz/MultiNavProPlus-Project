package com.example.multinav.bluetooth

    import android.annotation.SuppressLint
    import android.util.Log
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.viewModelScope
    import com.example.multinav.model.BluetoothDeviceData
    import com.example.multinav.model.BluetoothService
    import com.example.multinav.model.BluetoothUiState
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.TimeoutCancellationException
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
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

    // Track BLE module connection state separately
    private val _bleModuleConnected = MutableStateFlow(false)
    val bleModuleConnected: StateFlow<Boolean> = _bleModuleConnected

    // Add these at the class level with your other state variables
    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet: StateFlow<Boolean> = _showBottomSheet

    // Add this property to track BLE module scanning separately
    private val _isBleModuleScanning = MutableStateFlow(false)
    val isBleModuleScanning: StateFlow<Boolean> = _isBleModuleScanning


    // Add these controller methods
    fun showDeviceBottomSheet() {
        Log.d("BluetoothViewModel", "Showing bottom sheet")

        _showBottomSheet.value = true
    }

    fun hideDeviceBottomSheet() {
        _showBottomSheet.value = false
    }

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

        // Monitor connection status
        viewModelScope.launch {
            bluetoothService.connectionStatus.collectLatest { status ->
                when (status) {
                    is BluetoothService.ConnectionStatus.Connected -> {
                        val connectedDevice = bluetoothService.getConnectedDevice()
                        if (connectedDevice != null) {
                            // Determine if this is the BLE module or another device
                            // You'd need to set a way to identify your BLE module
                            _bleModuleConnected.value = isBleModule(connectedDevice)
                        }
                    }

                    is BluetoothService.ConnectionStatus.Disconnected -> {
                        _bleModuleConnected.value = false
                    }

                    else -> { /* Other states */
                    }
                }
            }
        }

        // Monitor scanned devices list
        viewModelScope.launch {
            bluetoothService.scannedDevicesList.collectLatest { devices ->
                Log.d("BluetoothViewModel", "Received updated device list: ${devices.size} devices")
                _uiState.update { state ->
                    state.copy(
                        scannedDevices = devices,
                        isScanning = false // Turn off scanning indicator when we get results
                    )
                }
            }
        }

        // Collect scanned devices from BLE
        viewModelScope.launch {
            bluetoothService.scannedDevicesFromBle.collectLatest { devices ->
                _uiState.update { state ->
                    state.copy(scannedDevicesFromBle = devices)
                }
            }
        }

        // Collect scanning state
        viewModelScope.launch {
            bluetoothService.isScanningState.collectLatest { isScanning ->
                _uiState.update { state ->
                    state.copy(isScanning = isScanning)
                }
            }
        }
    }

    // Helper to identify the BLE module - customize this based on your needs
    private fun isBleModule(device: BluetoothDeviceData): Boolean {
        // Check if this is your BLE module based on name, address, or other criteria
        return device.name?.contains("BLE", ignoreCase = true) ?: false ||
                device.address == "00:11:22:33:44:55" // Replace with your module's address
    }


    // Update requestBleModuleScan to use the BLE module scanning state
    fun requestBleModuleScan() {
        if (!_bleModuleConnected.value) {
            _uiState.update {
                it.copy(
                    errorMessage = "Not connected to BLE module. Connect first."
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                // Clear existing device list and set BLE module scanning state
                bluetoothService.clearScannedDevicesFromBle()
                _isBleModuleScanning.value = true

                _uiState.update {
                    it.copy(
                        scannedDevicesFromBle = emptyList(),
                        errorMessage = null
                    )
                }

                Log.d("BluetoothViewModel", "Requesting BLE module to scan for devices")
                val scanInitiated = bluetoothService.requestBleModuleScan()

                if (!scanInitiated) {
                    Log.e("BluetoothViewModel", "Failed to initiate scan on BLE module")
                    _isBleModuleScanning.value = false
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to initiate scan on BLE module"
                        )
                    }
                }

                // Monitor the scanning state from the service
                // This assumes bluetoothService.isScanning is specifically for BLE module scanning
                bluetoothService.isScanningState.collect { isScanning ->
                    _isBleModuleScanning.value = isScanning
                    if (!isScanning) {
                        // Scanning finished, break out of the collection
                        return@collect
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Error requesting BLE module scan", e)
                _isBleModuleScanning.value = false
                _uiState.update {
                    it.copy(
                        errorMessage = "Scan error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Connects to a device by index from the bottom sheet and navigates with the correct address
     */
    fun connectToDeviceByIndexAndNavigate(
        index: Int,
        onNavigate: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Get the current scanned devices list
                val devices = _uiState.value.scannedDevices

                // Validate index
                if (index < 0 || index >= devices.size) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Invalid device selection"
                        )
                    }
                    return@launch
                }

                // Get the device at the selected index
                val selectedDevice = devices[index]
                Log.d(
                    "BluetoothViewModel",
                    "Selected device at index $index: ${selectedDevice.name} (${selectedDevice.address})"
                )

                // Update UI to show we're connecting
                _uiState.update {
                    it.copy(
                        isConnecting = true,
                        errorMessage = null
                    )
                }

                // Connect to the device through the BLE module
                val success = bluetoothService.connectToDeviceByIndex(index)

                if (success) {
                    Log.d("BluetoothViewModel", "Successfully connected to device at index $index")

                    // Update the selected device as connected in the UI
                    _uiState.update { state ->
                        val updatedDevices = state.scannedDevices.mapIndexed { i, device ->
                            if (i == index) device.copy(isConnected = true) else device
                        }
                        state.copy(
                            isConnecting = false,
                            errorMessage = null,
                            scannedDevices = updatedDevices
                        )
                    }

                    // Store the device address for later reference
                    bluetoothService.saveLastConnectedAddress(selectedDevice.address)

                    // Hide the bottom sheet
                    hideDeviceBottomSheet()

                    // Navigate to the appropriate screen with the correct address
                    onNavigate(selectedDevice.address)
                } else {
                    Log.e("BluetoothViewModel", "Failed to connect to device at index $index")
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            errorMessage = "Failed to connect to selected device"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Error connecting to device by index", e)
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        errorMessage = "Connection error: ${e.message}"
                    )
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
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Failed to start server", e)
                _uiState.update { it.copy(errorMessage = "Failed to start server: ${e.message}") }
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun startScanning() {
        viewModelScope.launch {
            try {
                // Check if location services are enabled
                if (!bluetoothService.isLocationEnabled()) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = "Location services are disabled. Please enable location to scan for devices."
                        )
                    }
                    bluetoothService.enableLocation()
                    return@launch
                }
                disconnect()

                // Proceed with scanning if location is enabled
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
                    Log.d(
                        "BluetoothViewModel",
                        "Device found - Name: ${device.name ?: "Unknown"}, Address: ${device.address}, RSSI: $rssi, IsMobile: $isMobileDevice"
                    )
                    _uiState.update { state ->
                        val newDevice = BluetoothDeviceData(
                            name = device.name ?: "Unknown Device",
                            address = device.address,
                            rssi = rssi,
                            isConnected = false,
                            isMobileDevice = isMobileDevice,
                            clickCount = 0 // Initialize clickCount
                        )
                        if (!state.scannedDevices.any { it.address == newDevice.address }) {
                            Log.d(
                                "BluetoothViewModel",
                                "Adding new device to list: ${newDevice.name}"
                            )
                            state.copy(scannedDevices = state.scannedDevices + newDevice)
                        } else {
                            Log.d("BluetoothViewModel", "Device already in list: ${newDevice.name}")
                            state
                        }
                    }
                }
                Log.d("BluetoothViewModel", "Scan started, waiting 30 seconds...")
                delay(30000)
                Log.d("BluetoothViewModel", "30 seconds elapsed, stopping scan")
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
        isFromPairedList: Boolean
    ) {
        // Disconnect any existing connection and reset all devices' connection state
        bluetoothService.disconnect()
        _uiState.update { state ->
            state.copy(
                isConnecting = false,
                pairedDevices = state.pairedDevices.map {
                    it.copy(
                        isConnected = false,
                        clickCount = 0
                    )
                },
                scannedDevices = state.scannedDevices.map {
                    it.copy(
                        isConnected = false,
                        clickCount = 0
                    )
                }
            )
        }

        // Increment click count for the clicked device
        val updatedDevice = device.copy(clickCount = device.clickCount + 1)
        val clickCount = updatedDevice.clickCount

        // Update the device in the UI state with the new click count
        _uiState.update { state ->
            if (isFromPairedList) {
                state.copy(
                    pairedDevices = state.pairedDevices.map {
                        if (it.address == device.address) updatedDevice else it
                    }
                )
            } else {
                state.copy(
                    scannedDevices = state.scannedDevices.map {
                        if (it.address == device.address) updatedDevice else it
                    }
                )
            }
        }

        // Check if this is a BLE module device
        val deviceName = device.name ?: "Unknown"
        val isBleModule =
            true// deviceName.contains("BLE", ignoreCase = true) || deviceName == "BLE_WB07"

        // Determine isMobileDevice parameter for the connection
        if (isFromPairedList) {
            bluetoothService.isMobileDevice = true
        } else {
            bluetoothService.isMobileDevice = !isBleModule
        }

        Log.d(
            "BluetoothViewModel",
            "Device clicked: ${device.name}, Address: ${device.address}, Is BLE Module: $isBleModule, Click count: $clickCount"
        )

        if (clickCount == 1) {
            // First click: Attempt to connect
            deviceConnectionJob?.cancel()
            deviceConnectionJob = viewModelScope.launch {
                try {
                    _uiState.update { it.copy(isConnecting = true) }
                    withTimeout(10000) {
                        val success = bluetoothService.connectToDevice(
                            device.address,
                            bluetoothService.isMobileDevice
                        )
                        _uiState.update { state ->
                            if (success) {
                                if (isFromPairedList) {
                                    state.copy(
                                        isConnecting = false,
                                        pairedDevices = state.pairedDevices.map {
                                            if (it.address == device.address) it.copy(isConnected = true) else it
                                        },
                                        errorMessage = null
                                    )
                                } else {
                                    state.copy(
                                        isConnecting = false,
                                        scannedDevices = state.scannedDevices.map {
                                            if (it.address == device.address) it.copy(isConnected = true) else it
                                        },
                                        errorMessage = null
                                    )
                                }
                            } else {
                                state.copy(
                                    isConnecting = false,
                                    errorMessage = "Failed to connect"
                                )
                            }
                        }

                        if (success) {
                            // If this is a BLE module, scan for devices and show the bottom sheet
                            if (isBleModule) {
                                _bleModuleConnected.value = true
                                // Clear any existing scanned devices and request a new scan
                                _uiState.update { it.copy(scannedDevices = emptyList()) }
                                // Request scan from BLE module
                                requestBleModuleScan()
                                // Show the bottom sheet for device selection
                                showDeviceBottomSheet()
                                // Don't navigate - wait for device selection
                            } else {
                                // For regular devices, navigate to chat as usual
                                onNavigate()
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
                } catch (e: CancellationException) {
                    Log.d("BluetoothViewModel", "Connection cancelled", e)
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            errorMessage = null
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
        } else if (clickCount >= 2) {
            // Second click: Navigate to ChatScreen if connected
            val currentDevice = if (isFromPairedList) {
                _uiState.value.pairedDevices.find { it.address == device.address }
            } else {
                _uiState.value.scannedDevices.find { it.address == device.address }
            }

            if (currentDevice?.isConnected == true) {
                // If it's a BLE module and connected, show the bottom sheet again
                if (isBleModule) {
                    _bleModuleConnected.value = true
                    requestBleModuleScan()
                    showDeviceBottomSheet()
                } else {
                    // For regular devices, navigate to chat
                    onNavigate()
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Device is not connected. Please connect first.")
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothService.disconnect()
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    pairedDevices = it.pairedDevices.map { device ->
                        device.copy(isConnected = false, clickCount = 0)
                    },
                    scannedDevices = it.scannedDevices.map { device ->
                        device.copy(isConnected = false, clickCount = 0)
                    }
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
            return BluetoothViewModel(bluetoothService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}