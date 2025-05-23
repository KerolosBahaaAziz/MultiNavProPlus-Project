package com.example.widgets


import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.unit.dp
import com.example.multinav.ui.theme.gradientBrush

@Composable
fun RadioButtonMode(
    modifier: Modifier = Modifier,
    selectedModeState: MutableState<String>,
    modeName: String,
    onClick: () -> Unit = {},
    radioButtonColor: Color = Color.Unspecified  // Add this parameter with a default value
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        RadioButton(
            selected = selectedModeState.value == modeName,
            onClick = {
                selectedModeState.value = modeName
                onClick()
            },
            // Apply the color using colors parameter
            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor = radioButtonColor
            )
        )
        Text(
            modeName,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}