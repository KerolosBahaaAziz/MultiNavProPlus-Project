package com.example.multinav.bluetooth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.multinav.BluetoothDeviceData
import com.example.multinav.BluetoothUiState
import com.example.multinav.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDeviceScreen(
    state: BluetoothUiState,
    onDeviceClick: (BluetoothDeviceData) -> Unit,
    bluetoothViewModel: BluetoothViewModel = viewModel(),
    navController: NavController
) {
    val isServerMode = remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Devices") },
                actions = {
                    IconButton(
                        onClick = { bluetoothViewModel.startClient() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
//                    TextButton(
//                        onClick = {
//                            isServerMode.value = !isServerMode.value
//                            if (isServerMode.value) {
//                                bluetoothViewModel.startServer()
//                            } else {
//                                bluetoothViewModel.startClient()
//                            }
//                        }
//                    ) {
//                        Text(
//                            if (isServerMode.value) "Switch to Client" else "Switch to Server",
//                            color = Color.White
//                        )
//                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
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
                Text(if (state.isScanning) "Stop Scanning" else "Start Scanning")
            }
            if (state.isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
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
                                }
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
                                    }
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