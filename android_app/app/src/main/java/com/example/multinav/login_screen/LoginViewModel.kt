package com.example.multinav.login_screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel(){

    var username by mutableStateOf("")
    var password by  mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
}