package com.example.widgets

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multinav.ui.theme.violetPurple

@Composable
fun CircleToggleButton(
    modifier: Modifier = Modifier,
    isToggled : MutableState<Boolean>,
    buttonName:String ,onButtonClick: (Boolean) -> Unit,

) {

    Button(
        onClick = {
            isToggled.value = ! isToggled.value
            onButtonClick( isToggled.value)
        },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor =  if (isToggled.value) violetPurple else Color.Gray),
        modifier = Modifier.size(50.dp)
    ) {
        Text(
            text = buttonName,
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )


    }
}