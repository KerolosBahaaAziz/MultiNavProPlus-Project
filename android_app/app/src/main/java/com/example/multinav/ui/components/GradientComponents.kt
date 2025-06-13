// File: com.example.multinav.ui.components/GradientComponents.kt

package com.example.multinav.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.multinav.ui.theme.AppTheme

/**
 * A reusable surface component with gradient background.
 * Use this for any container that needs the app's gradient theme.
 *
 * @param modifier Modifier to be applied to the container
 * @param gradientBrush The gradient brush to use (defaults to app's horizontal gradient)
 * @param shape Shape of the container (defaults to none)
 * @param content Content to display within the gradient surface
 */
@Composable
fun GradientSurface(
    modifier: Modifier = Modifier,
    gradientBrush: Brush = AppTheme.horizontalGradient,
    shape: Shape = RoundedCornerShape(0.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = gradientBrush)
    ) {
        content()
    }
}

/**
 * A button with gradient background.
 *
 * @param onClick Action to perform when button is clicked
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 * @param gradientBrush The gradient to use
 * @param contentPadding Padding for the button content
 * @param content Content of the button
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradientBrush: Brush = AppTheme.horizontalGradient,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        GradientSurface(
            modifier = Modifier.fillMaxSize(),
            gradientBrush = gradientBrush,
            shape = MaterialTheme.shapes.small
        ) {
            Box(
                modifier = Modifier.padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

/**
 * A card with gradient background.
 *
 * @param modifier Modifier for the card
 * @param gradientBrush The gradient to use
 * @param shape Shape of the card
 * @param content Content of the card
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradientBrush: Brush = AppTheme.horizontalGradient,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        GradientSurface(
            modifier = Modifier.fillMaxSize(),
            gradientBrush = gradientBrush,
            shape = shape
        ) {
            content()
        }
    }
}

/**
 * A floating action button with gradient background.
 *
 * @param onClick Action to perform when FAB is clicked
 * @param modifier Modifier for the FAB
 * @param icon Icon to display in the FAB
 * @param contentDescription Content description for accessibility
 * @param gradientBrush The gradient to use
 */
@Composable
fun GradientFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    contentDescription: String? = null,
    gradientBrush: Brush = AppTheme.horizontalGradient
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color.Transparent,
        contentColor = AppTheme.onGradientColor,
        modifier = modifier
            .background(
                brush = gradientBrush,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}