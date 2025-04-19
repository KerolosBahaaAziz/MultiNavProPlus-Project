package com.example.multinav.add_delay_screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SetDelayViewModel : ViewModel() {
    val selectedSeconds = mutableStateOf(0)
    val selectedMilliseconds = mutableStateOf(0)
}