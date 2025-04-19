package com.example.multinav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.multinav.add_delay_screen.SetDelayScreen


import com.example.multinav.ui.theme.MultiNavTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MultiNavTheme {
            //    ChatScreen()
              //  JoyStickScreen()
                SetDelayScreen()
            }
        }
    }
}