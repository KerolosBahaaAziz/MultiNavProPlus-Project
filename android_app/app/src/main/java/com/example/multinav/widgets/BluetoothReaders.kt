package com.example.widgets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BluetoothReaders(bluetoothReader : String,bluetoothReaderType : String,modifier: Modifier = Modifier ) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = bluetoothReader,
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 16.dp,end = 1.dp)

        )
        Text(
            text = bluetoothReaderType,
            fontSize = 18.sp,
            modifier = Modifier.padding(end = 8.dp)

        )
    }
}