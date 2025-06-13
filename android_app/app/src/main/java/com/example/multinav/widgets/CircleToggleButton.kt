package com.example.widgets

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.multinav.ui.theme.violetPurple

@Composable
fun CircleToggleButton(
    modifier: Modifier = Modifier,
    isToggled: MutableState<Boolean>,
    buttonName: String,
    onButtonClick: (Boolean) -> Unit
) {
    val gradientColors = listOf(
        Color(0xFF233992),
        Color(0xFFA030C7),
        Color(0xFF1C0090)
    )
    Button(
        onClick = {
            isToggled.value = !isToggled.value
            onButtonClick(isToggled.value)
        },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent, // Changed to transparent to respect modifier's background
            contentColor = Color.White // Ensure text remains visible
        ),
        modifier = modifier
            .size(50.dp)
            .background(
                brush = if (isToggled.value) Brush.linearGradient(colors = gradientColors) else Brush.linearGradient(listOf(Color.Gray, Color.Gray)),
                shape = CircleShape
            )
    ) {
        Text(
            text = buttonName,
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}