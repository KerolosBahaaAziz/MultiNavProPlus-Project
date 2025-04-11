import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.multinav.chat.ChatViewModel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.multinav.chat.Message
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multinav.R


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel ()) {
    val messages by viewModel.messages.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat" , color = Color.Black)
                },
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
                messages.forEach { message ->
                    MessageBubble(message)
                }

                Spacer(modifier = Modifier.weight(1f))

                MessageInput(viewModel)
            }
        }
    )
}
@Composable
fun MessageBubble(message: Message) {
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
                    color = if (message.isSentByUser) Color(0xFF0A74DA) else Color(0xFF6C757D),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(8.dp)
        )
    }
}

@Composable
fun MessageInput(viewModel: ChatViewModel) {
    var inputText by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .weight(2f)
                .background(Color.White, shape = MaterialTheme.shapes.small)
                .padding(12.dp)


        )
        IconButton(onClick = {
            viewModel.sendMessage(inputText)
            inputText = ""
        } ) {
            Icon(
                painter = painterResource(R.drawable.ic_send),
                contentDescription = "Send",
                tint = Color.White
            )



        }

        IconButton(onClick = {
            viewModel.sendMessage(inputText)
            inputText = ""
        } ) {
            Icon(
                painterResource(R.drawable.ic_mic)
                , contentDescription = "Send",
                tint = Color.White
            )
        }

        IconButton(onClick = {
            viewModel.sendMessage(inputText)
            inputText = ""
        } ) {
            Icon(
                painterResource(R.drawable.ic_cal)
                , contentDescription = "Send" ,
                tint = Color.White
            )
        }
    }
}


class ChatViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}