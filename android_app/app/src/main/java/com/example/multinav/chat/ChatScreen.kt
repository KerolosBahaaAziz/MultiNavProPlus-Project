package com.example.multinav.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.multinav.BluetoothService
import com.example.multinav.R
import com.example.multinav.chat.ChatViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    deviceAddress: String? = null,
    bluetoothService: BluetoothService,
    onNavigateBack: () -> Unit = {},
    viewModel: ChatViewModel
) {
    val messages by viewModel.messages.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    LaunchedEffect(messages) {
        Log.d("ChatScreen", "Messages in UI: $messages")
    }
    // Log connection state for debugging
    LaunchedEffect(connectionState) {
        println("ChatScreen: Connection state changed to $connectionState")
    }

    // Auto-scroll to bottom when new messages arrive
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bluetoothService.connectedDeviceName ?: "Chat",
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = when (connectionState) {
                                        is BluetoothService.ConnectionStatus.Connected -> Color.Green
                                        is BluetoothService.ConnectionStatus.Connecting -> Color.Yellow
                                        is BluetoothService.ConnectionStatus.Error -> Color.Red
                                        else -> Color.Gray
                                    },
                                    shape = CircleShape
                                )
                        )
                    }


                }
            )
            }
        )
                    { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF233992),
                            Color(0xFFA030C7),
                            Color(0xFF1C0090)
                        )
                    )
                )
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(messages) { message ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = if (message.isSentByUser)
                            Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Text(
                            text = message.text, // Display the message as-is
                            color = Color.White,
                            modifier = Modifier
                                .background(
                                    color = if (message.isSentByUser) Color(0xFF0A74DA)
                                    else Color(0xFF6C757D),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            MessageInput(
                viewModel = viewModel,
                enabled = connectionState is BluetoothService.ConnectionStatus.Connected
            )
        }
    }
}

@Composable
fun MessageInput(viewModel: ChatViewModel, enabled: Boolean = true) {
    var inputText by rememberSaveable { mutableStateOf("") }

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
                    val bytes = inputText.toByteArray(Charsets.UTF_8)
                    Log.d("ChatScreen", "Sending message: $inputText, Bytes: ${bytes.joinToString()}")
                    viewModel.sendMessage(inputText)
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
//            Icon(
//                painter = painterResource(R.drawable.ic_cal),
//                contentDescription = "Call",
//                tint = if (enabled) Color(0xFF0A74DA) else Color.Gray
//            )
        }
    }
}

