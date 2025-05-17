package com.example.multinav.bluetooth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.multinav.model.bluetooth_service.BluetoothDeviceData
import com.example.multinav.model.bluetooth_service.BluetoothUiState
import com.example.multinav.Screen
import com.example.multinav.ui.components.GradientButton
import com.example.multinav.ui.theme.AppTheme
import com.example.multinav.ui.theme.AppTheme.gradientColors
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
    val isBleModuleScanning by bluetoothViewModel.isBleModuleScanning.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF233992).copy(alpha = 0.95f),  // Very subtle at top
                        Color(0xFFA030C7).copy(alpha = 0.95f),   // Subtle in middle
                        Color(0xFF1C0090).copy(alpha = 0.95f)   // Slightly more visible at bottom
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent, // Set container color to transparent
            contentColor = contentColorFor(MaterialTheme.colorScheme.background) ,// Keep default content color
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = Brush.horizontalGradient(colors = gradientColors))

                ) {
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
                            containerColor = Color.Transparent,  // Make container transparent so gradient shows
                            titleContentColor = Color.White
                        )
                    )
                }
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
//                //btn settings
//                GradientButton(
//                    text = "Open Bluetooth Settings",
//                    onClick = bluetoothViewModel::openBluetoothSettings,
//                    modifier = Modifier.fillMaxWidth()
//                )
//                Spacer(modifier = Modifier.height(16.dp))

                    //btn scanning
                    GradientButton(
                        text = if (state.isScanning) "Stop Scanning" else "Scan BLE Devices",
                        onClick = {
                            if (state.isScanning) {
                                bluetoothViewModel.stopScanning()
                            } else {
                                bluetoothViewModel.startScanning()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = state.isScanning
                    )

                    Spacer(modifier = Modifier.height(16.dp))


                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (true)//state.scannedDevices.isNotEmpty() )
                        {
                            item {
                                Text(
                                    text = "Scanned Devices (${state.scannedDevices.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp),
                                    color = Color.White
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


                    // When showing the bottom sheet, use the BLE module scanning state:
                    if (showBottomSheet) {
                        BleDeviceBottomSheet(
                            devices = state.scannedDevicesFromBle,
                            isScanning = state.isBleModuleScanning,
                            statusMessage = state.statusMessage,
                            scanCompleted = state.scanCompleted,
                            onScanRequest = { bluetoothViewModel.requestBleModuleScan() },
                            onDeviceSelected = { index ->
                                bluetoothViewModel.connectToDeviceByIndexAndNavigate(
                                    index = index,
                                    onNavigate = { deviceAddress ->
                                        navController.navigate(Screen.Chat.createRoute(deviceAddress))
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


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    fun BleDeviceBottomSheet(
        devices: List<BluetoothDeviceData>,
        isScanning: Boolean,
        statusMessage: String?,
        scanCompleted: Boolean,
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
                // Header with improved styling
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.CenterStart)) {
                        Text(
                            text = "BLE Connections",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        statusMessage?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                // Scan button with simple text change when scanning
                GradientButton (
                    onClick = onScanRequest,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isScanning, // Disable while scanning
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = MaterialTheme.colorScheme.primary,
//                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
//                    )
                ) {
                    Text(if (isScanning) "Scanning..." else "Scan for Devices")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simplified status indicator - only show when scanning with no devices
                if (isScanning && devices.isEmpty()) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                // Device list section
                if (devices.isNotEmpty()) {
                    Column {
                        // Found devices header with count - simplified
                        Text(
                            text = if (isScanning)
                                "Devices Found So Far (${devices.size})"
                            else
                                "Available Devices (${devices.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Device list
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 350.dp)
                        ) {
                            itemsIndexed(devices) { index, device ->
                                Card(
                                    onClick = { onDeviceSelected(index) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .animateItemPlacement(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 2.dp
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "#$index: ${device.name}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = device.address,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Simple text indicator if still scanning
                        if (isScanning) {
                            Text(
                                text = "Looking for more devices...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // Empty state - only show when not scanning and no devices found
                else if (!isScanning && devices.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Text(
                            text = "No devices found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Try scanning again or checking that devices are in range",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                        )
                    }
                }
                // Simplified scanning state - show only when scanning with no devices yet
                else if (isScanning && devices.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Text(
                            text = "Scanning for devices...",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "This may take a few moments",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
