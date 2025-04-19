package com.example.multinav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDeviceScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,

    onDeviceClick: (BluetoothDeviceData) -> Unit,
    bluetoothViewModel: BluetoothViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Devices") },
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
                    Text("Search & Pair Devices")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Paired Devices Section
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
                        onClick = { onDeviceClick(device) }
                    )
                }
            }

            // Error Message
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
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun BluetoothDeviceScreen(
//    state: BluetoothUiState,
//    onStartScan: () -> Unit,
//    onStopScan: () -> Unit,
//    onDeviceClick: (BluetoothDeviceData) -> Unit
//) {
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Bluetooth Devices") },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    titleContentColor = Color.White
//                )
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp)
//        ) {
//            // Scan Button with loading indicator
//            Button(
//                onClick = { if (state.isScanning) onStopScan() else onStartScan() },
//                modifier = Modifier.fillMaxWidth(),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = if (state.isScanning)
//                        MaterialTheme.colorScheme.error
//                    else
//                        MaterialTheme.colorScheme.primary
//                )
//            ) {
//                Row(
//                    horizontalArrangement = Arrangement.Center,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    if (state.isScanning) {
//                        CircularProgressIndicator(
//                            modifier = Modifier.size(20.dp),
//                            color = Color.White
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                    }
//                    Text(if (state.isScanning) "Scanning..." else "Start Scan")
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Paired Devices Section with count
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Paired Devices",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold
//                )
//                Text(
//                    text = "(${state.pairedDevices.size})",
//                    style = MaterialTheme.typography.bodyMedium
//                )
//            }
//
//            if (state.pairedDevices.isEmpty()) {
//                Text(
//                    text = "No paired devices",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    modifier = Modifier.padding(vertical = 8.dp)
//                )
//            }
//
//            LazyColumn(
//                modifier = Modifier.weight(1f)
//            ) {
//                items(state.pairedDevices) { device ->
//                    DeviceItem(
//                        device = device,
//                        onClick = { onDeviceClick(device) }
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Available Devices Section with count and scanning indicator
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Available Devices",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold
//                )
//                Text(
//                    text = "(${state.scannedDevices.size})",
//                    style = MaterialTheme.typography.bodyMedium
//                )
//            }
//
//            if (state.scannedDevices.isEmpty()) {
//                Text(
//                    text = if (state.isScanning)
//                        "Searching for devices..."
//                    else
//                        "No devices found",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    modifier = Modifier.padding(vertical = 8.dp)
//                )
//            }
//
//            LazyColumn(
//                modifier = Modifier.weight(1f)
//            ) {
//                items(state.scannedDevices) { device ->
//                    DeviceItem(
//                        device = device,
//                        onClick = { onDeviceClick(device) }
//                    )
//                }
//            }
//
//            // Error Message
//            state.errorMessage?.let { error ->
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 8.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.errorContainer
//                    )
//                ) {
//                    Text(
//                        text = error,
//                        color = MaterialTheme.colorScheme.onErrorContainer,
//                        modifier = Modifier.padding(16.dp)
//                    )
//                }
//            }
//        }
//    }
//}
//
