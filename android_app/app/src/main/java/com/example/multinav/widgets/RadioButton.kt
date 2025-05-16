package com.example.widgets


import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.unit.dp


@Composable
fun RadioButtonMode(
    modifier: Modifier = Modifier,
    selectedModeState: MutableState<String>,
    modeName :String,
    onClick: () -> Unit = {} ) {
    val gradientColors = listOf(
        Color(0xFF233992),
        Color(0xFFA030C7),
        Color(0xFF1C0090)
    )
   Row(
       verticalAlignment = Alignment.CenterVertically,
   ) {
       RadioButton(
           selected = selectedModeState.value == modeName,
           onClick =  {
               selectedModeState.value = modeName
               onClick()
           }
       )
       Text(
           modeName,
           modifier = Modifier.padding(start = 2.dp),
       )

   }


}

