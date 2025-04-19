
package com.example.multinav

import BluetoothService
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

//    class BluetoothViewModel(
//        private val bluetoothService: BluetoothService
//    ): ViewModel() {
//
//        private val TAG = "Bluetooth"
//
//        private val _state = MutableStateFlow(BluetoothUiState())
//        val state = _state.asStateFlow()
//
//        private var deviceConnectionJob: Job? = null
//
//        init {
//            updatePairedDevices()
//            viewModelScope.launch {
//                bluetoothService.scannedDevices.collect { devices ->
//                    Log.d(TAG, "Scanned devices updated: ${devices.size} devices")
//                    _state.update { it.copy(
//                        scannedDevices = devices,
//                        isScanning = devices.isNotEmpty()
//                    ) }
//                }
//            }
//        }
//
//        fun startScan() {
//            Log.d(TAG, "Starting scan")
//            _state.update { it.copy(isScanning = true) }
//            bluetoothService.startDiscovery()
//        }
//
//        fun stopScan() {
//            Log.d(TAG, "Stopping scan")
//            _state.update { it.copy(isScanning = false) }
//            bluetoothService.stopDiscovery()
//        }
//
//        fun connectToDevice(device: BluetoothDeviceData, onSuccess: () -> Unit) {
//            deviceConnectionJob?.cancel()
//            deviceConnectionJob = viewModelScope.launch {
//                _state.update { it.copy(isConnecting = true) }
//                try {
//                    if (bluetoothService.pairDevice(device)) {
//                        updatePairedDevices()
//                        _state.update { it.copy(
//                            isConnecting = false,
//                            isConnected = true,
//                            errorMessage = null
//                        ) }
//                        onSuccess()
//                    } else {
//                        _state.update { it.copy(
//                            isConnecting = false,
//                            isConnected = false,
//                            errorMessage = "Failed to pair with device"
//                        ) }
//                    }
//                } catch(e: Exception) {
//                    _state.update { it.copy(
//                        isConnecting = false,
//                        isConnected = false,
//                        errorMessage = e.message
//                    ) }
//                }
//            }
//        }
//
//        private fun updatePairedDevices() {
//            _state.update { it.copy(
//                pairedDevices = bluetoothService.getPairedDevices()
//            ) }
//        }
//
//        override fun onCleared() {
//            super.onCleared()
//            bluetoothService.stopDiscovery()
//        }
//    }
//



class BluetoothViewModel(
    private val bluetoothService: BluetoothService
): ViewModel() {
            private var deviceConnectionJob: Job? = null
            private val TAG = "Bluetooth"

        private val _state = MutableStateFlow(BluetoothUiState())
        val state = _state.asStateFlow()

    fun openBluetoothSettings() {
        bluetoothService.openBluetoothSettings()
    }

    // Update connectToDevice to only handle paired devices
    fun connectToDevice(device: BluetoothDeviceData, onSuccess: () -> Unit) {
        deviceConnectionJob?.cancel()
        deviceConnectionJob = viewModelScope.launch {
            if (state.value.pairedDevices.any { it.address == device.address }) {
                _state.update { it.copy(isConnected = true) }
                onSuccess()
            } else {
                openBluetoothSettings()
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