package com.example.joystick_Screen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class JoyStickViewModel : ViewModel() {
    val selectedMode =  mutableStateOf("")
    val isToggleButtonA = mutableStateOf(false)
    val isToggleButtonB = mutableStateOf(false)
    val isToggleButtonC = mutableStateOf(false)
    val isToggleButtonD = mutableStateOf(false)

}