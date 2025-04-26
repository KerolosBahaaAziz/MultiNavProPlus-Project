package com.example.widgets

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.multinav.ui.theme.violetPurple

@Composable
fun FloatingButton(modifier: Modifier = Modifier ,onClick: () -> Unit) {
    FloatingActionButton(
            onClick = { onClick() },
            shape = CircleShape,
        containerColor = violetPurple
        ) {
            Icon(Icons.Filled.Add, "floating action button")
        }
    }