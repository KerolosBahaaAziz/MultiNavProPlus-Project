package com.example.multinav.chat

import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multinav.AudioRecorder
import com.example.multinav.BluetoothService
import com.example.multinav.R
import com.example.multinav.chat.ChatViewModel
import java.io.File
import java.io.FileOutputStream


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
    val context = LocalContext.current

    val audioRecorder = remember { AudioRecorder(context) }

    // Initialize the ViewModel with AudioRecorder
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            deviceAddress = deviceAddress,
            bluetoothService = bluetoothService,
            isMobileDevice = false,
            audioRecorder = audioRecorder
        )
    )

    var currentMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("ChatScreen", "RECORD_AUDIO permission granted")
            // Permission granted, can proceed with recording
        } else {
            Log.w("ChatScreen", "RECORD_AUDIO permission denied")
            // Optionally, show a message to the user
        }
    }

    // Cleanup AudioRecorder when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            audioRecorder.release()
        }
    }

    LaunchedEffect(Unit) {
        // Check and request RECORD_AUDIO permission on start
        val permission = android.Manifest.permission.RECORD_AUDIO
        val context = context
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        }
    }

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
    ) { padding ->
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
                    when (message) {
                        is Message.Text -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = if (message.isSentByUser)
                                    Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Text(
                                    text = message.text,
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
                        is Message.Voice -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = if (message.isSentByUser)
                                    Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                VoiceMessageItem(
                                    audioBytes = message.audioBytes,
                                    isSentByUser = message.isSentByUser,
                                    currentMediaPlayer = currentMediaPlayer,
                                    onMediaPlayerChange = { newMediaPlayer ->
                                        currentMediaPlayer?.release()
                                        currentMediaPlayer = newMediaPlayer
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            MessageInput(
                viewModel = viewModel,
                enabled = connectionState is BluetoothService.ConnectionStatus.Connected,
                //audioRecorder = audioRecorder
            )
        }
    }
}


@Composable
fun VoiceMessageItem(
    audioBytes: ByteArray,
    isSentByUser: Boolean,
    currentMediaPlayer: MediaPlayer?,
    onMediaPlayerChange: (MediaPlayer?) -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .background(
                color = if (isSentByUser) Color(0xFF0A74DA) else Color(0xFF6C757D),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play Voice Message",
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    if (isPlaying) {
                        currentMediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        val tempFile = File(context.cacheDir, "temp_voice_message.3gp")
                        FileOutputStream(tempFile).use { it.write(audioBytes) }

                        val newMediaPlayer = MediaPlayer().apply {
                            setDataSource(tempFile.absolutePath)
                            setOnCompletionListener {
                                isPlaying = false
                                onMediaPlayerChange(null)
                            }
                            setOnPreparedListener {
                                start()
                                isPlaying = true
                            }
                            prepareAsync()
                        }
                        onMediaPlayerChange(newMediaPlayer)
                        tempFile.deleteOnExit()
                    }
                }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Voice Message",
            color = Color.White
        )
    }
}

@Composable
fun MessageInput(
    viewModel: ChatViewModel,
    enabled: Boolean = true
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val isRecording by viewModel.isRecording
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (enabled) Color.White else Color.LightGray,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp),
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

        // Text message send button
        IconButton(
            onClick = {
                if (inputText.isNotEmpty()) {
                    val bytes = inputText.toByteArray(Charsets.UTF_8)
                    Log.d(
                        "ChatScreen",
                        "Sending message: $inputText, Bytes: ${bytes.joinToString()}"
                    )
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            },
            enabled = enabled && inputText.isNotEmpty()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_send),
                contentDescription = "Send",
                tint = if (enabled && inputText.isNotEmpty()) Color(0xFF0A74DA) else Color.Gray
            )
        }

        // Voice recording buttons
        if (isRecording) {
            // Show Send and Cancel icons during recording
            IconButton(
                onClick = {
                    viewModel.sendVoiceMessage()
                },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Voice Message",
                    tint = if (enabled) Color(0xFF0A74DA) else Color.Gray
                )
            }
            IconButton(
                onClick = {
                    viewModel.cancelRecording()
                },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel Recording",
                    tint = if (enabled) Color.Red else Color.Gray
                )
            }
        } else {
            // Show Mic icon when not recording
            IconButton(
                onClick = {
                    val permission = android.Manifest.permission.RECORD_AUDIO
                    if (ContextCompat.checkSelfPermission(
                            context,
                            permission
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.startRecording()
                    } else {
                        Log.w("ChatScreen", "RECORD_AUDIO permission not granted")
                        viewModel.receiveMessage("Please grant RECORD_AUDIO permission to record voice messages")
                    }
                },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Record Voice",
                    tint = if (enabled) Color(0xFF0A74DA) else Color.Gray
                )
            }
        }
    }
}