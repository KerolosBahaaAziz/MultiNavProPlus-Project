package com.example.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FloatingButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val gradientColors = listOf(
        Color(0xFF233992),
        Color(0xFFA030C7),
        Color(0xFF1C0090)
    )

    FloatingActionButton(
        onClick = { onClick() },
        shape = CircleShape,
        modifier = modifier
            .padding(top = 10.dp)
            .background(
                brush = Brush.horizontalGradient(colors = gradientColors),
                shape = CircleShape
            ),
        containerColor = Color.Transparent // Set to transparent to show the gradient
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "floating action button",
            tint = Color.White // Adjust for visibility against gradient
            , modifier = Modifier.size(18.dp)
        )
    }
}