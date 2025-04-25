package com.example.widgets

import android.media.Image
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.example.multinav.ui.theme.violetPurple

@Composable
fun CircleIconButton(
    icon: ImageVector,

    contentDescription: String?,
    onCircleButtonClick: () -> Unit,
    size: Dp = 50.dp,
    iconSize: Dp = 24.dp){
    Button(
        onClick = onCircleButtonClick,
        shape = CircleShape,
        modifier = Modifier.size(size),
        colors = ButtonDefaults.buttonColors(containerColor = violetPurple),
        contentPadding = PaddingValues(8.dp)
    ){
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}
@Composable
fun CircleIconButton(
    image : Painter,
    contentDescription: String?,
    onCircleButtonClick: () -> Unit,
    size: Dp = 50.dp,
    iconSize: Dp = 24.dp){
    Button(
        onClick = onCircleButtonClick,
        shape = CircleShape,
        modifier = Modifier.size(size),
        contentPadding = PaddingValues(0.dp)
    ){
        Image(
            painter = image,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
            )
    }
}