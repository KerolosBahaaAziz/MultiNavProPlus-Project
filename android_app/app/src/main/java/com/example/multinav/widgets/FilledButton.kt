package com.example.widgets

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FiledButton(modifier: Modifier = Modifier, onClick:( )-> Unit,buttonName :String) {

    Button(
        onClick = onClick ){
        Text(
            text = buttonName,
            fontSize = 18.sp
        )
    }
}