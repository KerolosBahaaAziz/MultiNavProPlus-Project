package com.example.multinav.joystick_Screen
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MyAnalogJoystick(
    modifier: Modifier = Modifier,
    onAngleChange: (Float) -> Unit // Callback to provide the angle (0 to 360)
) {
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var angle by remember { mutableStateOf(0f) }
    val joystickRadius = 50.dp // Adjust as needed

    Canvas(
        modifier = modifier
            .size(200.dp)
            .pointerInput(Unit) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val center = Offset(centerX, centerY)

                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        touchPosition = offset
                        calculateAngle360(center, touchPosition) { newAngle ->
                            angle = newAngle
                            onAngleChange(angle)
                        }
                    },
                    onDrag = { change, _ ->
                        touchPosition = change.position
                        calculateAngle360(center, touchPosition) { newAngle ->
                            angle = newAngle
                            onAngleChange(angle)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        angle = 0f
                        onAngleChange(angle)
                    }
                )
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val center = Offset(centerX, centerY)

        // Draw the base of the joystick
        drawCircle(
            color = androidx.compose.ui.graphics.Color.Gray,
            radius = joystickRadius.toPx(),
            center = center
        )

        // Draw the movable part
        if (isDragging) {
            drawCircle(
                color = androidx.compose.ui.graphics.Color.Blue,
                radius = 20.dp.toPx(),
                center = touchPosition.coerceInCircle(center, joystickRadius.toPx())
            )
        } else {
            drawCircle(
                color = androidx.compose.ui.graphics.Color.LightGray,
                radius = 20.dp.toPx(),
                center = center
            )
        }
    }
}

private fun calculateAngle360(center: Offset, touch: Offset, onAngleCalculated: (Float) -> Unit) {
    val deltaX = touch.x - center.x
    val deltaY = touch.y - center.y

    // Use atan2 to get the angle in radians (-PI to PI)
    val radians = atan2(deltaY, deltaX).toDouble()

    // Convert radians to degrees (0 to 360)
    var degrees = Math.toDegrees(radians).toFloat()
    if (degrees < 0) {
        degrees += 360f
    }

    onAngleCalculated(degrees)
}

// Extension function to constrain a point within a circle
private fun Offset.coerceInCircle(center: Offset, radius: Float): Offset {
    val dx = x - center.x
    val dy = y - center.y
    val distanceSquared = dx * dx + dy * dy
    if (distanceSquared > radius * radius) {
        val angle = atan2(dy, dx)
        return Offset(
            center.x + radius * cos(angle),
            center.y + radius * sin(angle)
        )
    }
    return this
}

