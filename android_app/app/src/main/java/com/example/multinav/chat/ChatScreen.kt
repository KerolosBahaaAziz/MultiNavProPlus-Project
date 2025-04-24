import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.multinav.chat.ChatViewModel

import com.example.multinav.chat.Message
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multinav.BluetoothService
import com.example.multinav.R
import com.example.multinav.chat.ChatViewModelFactory
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Refresh


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    deviceAddress: String? = null,
    bluetoothService: BluetoothService,
    onNavigateBack: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(deviceAddress, bluetoothService)
    )
) {
    val messages by viewModel.messages.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Chat", color = Color.Black)

                        Spacer(modifier = Modifier.width(8.dp))

                        // Connection status indicator
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = when (connectionState) {
                                        is ChatViewModel.ConnectionState.Connected -> Color.Green
                                        is ChatViewModel.ConnectionState.Connecting -> Color.Yellow
                                        is ChatViewModel.ConnectionState.Error -> Color.Red
                                        else -> Color.Gray
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                },
                actions = {
                    // Add retry connection button when disconnected or error
                    if (connectionState is ChatViewModel.ConnectionState.Disconnected ||
                        connectionState is ChatViewModel.ConnectionState.Error
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.receiveMessage("Attempting to reconnect...")

                                deviceAddress?.let { viewModel.connectToDevice(it) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh, // Use a refresh icon
                                contentDescription = "Retry Connection",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF233992), // Dark Blue
                                Color(0xFFA030C7), // Purple
                                Color(0xFF1C0090)  // Magenta/Pink
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Messages list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(messages.filter { it.text.startsWith("BLE:") }) { message ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = if (message.isSentByUser)
                                Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            val displayText = message.text.removePrefix("BLE:")
                            Text(
                                text = displayText,
                                color = Color.White,
                                modifier = Modifier
                                    .background(
                                        color = when {
                                            message.isSentByUser -> Color(0xFF0A74DA)
                                            else -> Color(0xFF6C757D)
                                        },
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input area (disabled when not connected)
                MessageInput(
                    viewModel = viewModel,
                    enabled = connectionState is ChatViewModel.ConnectionState.Connected
                )


            }
        }
    )
}


@Composable
fun BLEMessageBubble(message: Message) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (message.isSentByUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = message.text,
            color = Color.White,
            modifier = Modifier
                .background(
                    color = when {
                        message.isSentByUser -> Color(0xFF0A74DA)    // Blue for sent messages
                        else -> Color(0xFF6C757D)                    // Gray for received messages
                    },
                    shape = MaterialTheme.shapes.medium
                )
                .padding(8.dp)
        )
    }
}

@Composable
fun MessageInput(viewModel: ChatViewModel, enabled: Boolean = true) {
    var inputText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (enabled) Color.White else Color.LightGray,
                shape = MaterialTheme.shapes.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                enabled = enabled
            )

            if (inputText.isEmpty()) {
                Text(
                    text = if (enabled) "Type a message..." else "Not connected",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        IconButton(
            onClick = {
                if (inputText.isNotEmpty()) {
                    val bleCommand = formatBLECommand(inputText)
                    viewModel.sendMessage(bleCommand)
                    inputText = ""
                }
            },
            enabled = enabled && inputText.isNotEmpty()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_send),
                contentDescription = "Send",
                tint = if (enabled) Color(0xFF0A74DA) else Color.Gray
            )
        }

        IconButton(
            onClick = { viewModel.sendVoice() },
            enabled = enabled
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = "Mic",
                tint = if (enabled) Color(0xFF0A74DA) else Color.Gray
            )
        }

        IconButton(
            onClick = { viewModel.makePhoneCall() },
            enabled = enabled
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cal),
                contentDescription = "Call",
                tint = if (enabled) Color(0xFF0A74DA) else Color.Gray
            )
        }
    }
}

private fun formatBLECommand(input: String): String {
    // Format command for your BLE device
    // Example: Convert "LED ON" to "BLE:LED_ON"
    return "BLE:${input.uppercase().replace(" ", "_")}"
}