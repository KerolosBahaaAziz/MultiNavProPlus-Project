package com.example.multinav.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Message(val text: String, val isSentByUser: Boolean)

class ChatViewModel(
    private val deviceAddress: String? = null
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(
        listOf(
            Message("Hello!", false),
            Message("Hi there!", true)
        )
    )

       val messages: StateFlow<List<Message>> = _messages

    fun sendMessage(message: String) {
        viewModelScope.launch {
            _messages.value = _messages.value + Message(message, true)
        }
    }

    fun receiveMessage(message: String) {
        viewModelScope.launch {
            _messages.value = _messages.value + Message(message, false)
        }
    }

    fun sendVoice() {

    }

    fun makePhoneCall() {
        TODO("Not yet implemented")
    }
}

class ChatViewModelFactory(
    private val deviceAddress: String? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(deviceAddress) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}