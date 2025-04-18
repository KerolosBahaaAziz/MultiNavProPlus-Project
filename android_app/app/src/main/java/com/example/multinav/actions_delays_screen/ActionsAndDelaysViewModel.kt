package com.example.desgin.actions_delays_screen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class ActionsAndDelaysViewModel : ViewModel() {

    val textFiled = mutableStateOf("")
    val selectedMode =  mutableStateOf("")
    val isToggleButtonA = mutableStateOf(false)
    val isToggleButtonB = mutableStateOf(false)
    val isToggleButtonC = mutableStateOf(false)
    val isToggleButtonD = mutableStateOf(false)
}