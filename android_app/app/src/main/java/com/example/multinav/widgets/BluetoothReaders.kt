package com.example.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BluetoothReaders(
    bluetoothReader: String,
    bluetoothReaderType: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(vertical = 8.dp, horizontal = 4.dp), // Reduced padding for balance
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center // Center the text within the Row
    ) {
        Text(
            text = bluetoothReader,
            fontSize = 22.sp,
            textAlign = TextAlign.Center, // Center the text
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = bluetoothReaderType,
            fontSize = 22.sp,
            textAlign = TextAlign.Center, // Center the text
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}