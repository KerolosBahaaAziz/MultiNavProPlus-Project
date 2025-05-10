package com.example.multinav.bluetooth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.multinav.model.BluetoothDeviceData
import com.example.multinav.model.BluetoothUiState
import com.example.multinav.Screen
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDeviceScreen(
    state: BluetoothUiState,
    bluetoothViewModel: BluetoothViewModel = viewModel(),
    navController: NavController,
    auth: FirebaseAuth
) {
    val isServerMode = remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = state.isScanning)

    val showBottomSheet by bluetoothViewModel.showBottomSheet.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Devices") },
                actions = {
                    IconButton(
                        onClick = {
                            auth.signOut()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.DeviceList.route) { inclusive = true }
                                popUpTo(Screen.JoyStick.route) { inclusive = true }
                                popUpTo(Screen.Chat.route) { inclusive = true }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Log Out",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                coroutineScope.launch {
                    delay(1500)
                    bluetoothViewModel.refreshDevices()
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (!state.isBluetoothEnabled) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Bluetooth is disabled. Please enable it to see paired devices.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                //btn settings
                Button(
                    onClick = bluetoothViewModel::openBluetoothSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Open Bluetooth Settings")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                //btn scanning
                Button(
                    onClick = {
                        if (state.isScanning) {
                            bluetoothViewModel.stopScanning()
                        } else {
                            bluetoothViewModel.startScanning()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (state.isScanning) "Stop Scanning" else " Scan Ble Devices")
                    }
                    if (state.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(30.dp)
                                .padding(8.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Paired Devices (${state.pairedDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.pairedDevices) { device ->
                        DeviceItem(
                            device = device,
                            onClick = {
                                bluetoothViewModel.connectToDeviceAndNavigate(
                                    device = device,
                                    onNavigate = {
                                        navController.navigate(
                                            Screen.Chat.createRoute(device.address)
                                        )
                                    },
                                    isFromPairedList = true
                                )
                            }
                        )
                    }
                    if (state.scannedDevices.isNotEmpty()) {
                        item {
                            Text(
                                text = "Scanned Devices (${state.scannedDevices.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                        items(state.scannedDevices) { device ->
                            DeviceItem(
                                device = device,
                                onClick = {
                                    bluetoothViewModel.connectToDeviceAndNavigate(
                                        device = device,
                                        onNavigate = {
                                            navController.navigate(
                                                Screen.Chat.createRoute(device.address)
                                            )
                                        },
                                        isFromPairedList = false
                                    )
                                }
                            )
                        }
                    }
                }
                state.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }


                // Display the bottom sheet when needed - OUTSIDE the Scaffold
                if (showBottomSheet) {
                    BleDeviceBottomSheet(
                        devices = state.scannedDevices,
                        isScanning = state.isScanning,
                        onScanRequest = { bluetoothViewModel.requestBleModuleScan() },
                        onDeviceSelected = { index ->
                            bluetoothViewModel.connectToDeviceByIndex(
                                index = index,
                                onSuccess = {
                                    bluetoothViewModel.hideDeviceBottomSheet()
                                    navController.navigate(Screen.Chat.createRoute(
                                        state.scannedDevices[index].address
                                    ))
                                }
                            )
                        },
                        onDismiss = { bluetoothViewModel.hideDeviceBottomSheet() }
                    )
                }

            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceItem(
    device: BluetoothDeviceData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                device.rssi?.let {
                    Text(
                        text = "RSSI: $it dBm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (device.isConnected) {
                Text(
                    text = "Connected",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleDeviceBottomSheet(
    devices: List<BluetoothDeviceData>,
    isScanning: Boolean,
    onScanRequest: () -> Unit,
    onDeviceSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BLE Module Devices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            // Scan button
            Button(
                onClick = onScanRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scan for Devices")
                    if (isScanning) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Device list or placeholder
            if (devices.isEmpty()) {
                if (!isScanning) {
                    Text(
                        text = "No devices found. Try scanning again.",
                        modifier = Modifier.padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Scanning for devices...",
                        modifier = Modifier.padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Available Devices (${devices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 350.dp)
                ) {
                    itemsIndexed(devices) { index, device ->
                        Card(
                            onClick = { onDeviceSelected(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "#$index: ${device.name ?: "Unknown Device"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = device.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                device.rssi?.let {
                                    Text(
                                        text = "Signal: $it dBm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}