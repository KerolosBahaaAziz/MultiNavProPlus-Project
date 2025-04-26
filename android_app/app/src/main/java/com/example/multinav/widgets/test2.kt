//package com.example.widgets
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.gestures.detectDragGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.center
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.input.pointer.consumeAllChanges
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import kotlin.math.atan2
//import kotlin.math.cos
//import kotlin.math.sin
//
//@Composable
//fun AngleKnob(
//    modifier: Modifier = Modifier,
//    initialAngle: Float = 0f,
//    onAngleChanged: (Float) -> Unit
//) {
//    // Use state to keep track of the total accumulated angle and previous pointer angle.
//    var totalAngle by remember { mutableStateOf(initialAngle) }
//    var previousAngle by remember { mutableStateOf<Float?>(null) }
//
//    Box(
//        modifier = modifier
//            .pointerInput(Unit) {
//                detectDragGestures(
//                    onDragStart = { offset ->
//                        // Calculate the initial angle from the center of the box.
//                        val center = size.center
//                        val touchOffset = offset - center
//                        previousAngle = Math.toDegrees(atan2(touchOffset.y, touchOffset.x).toDouble()).toFloat()
//                    },
//                    onDrag = { change, _ ->
//                        // Get the center of the knob.
//                        val center = size.center
//                        // Calculate the current angle using the touch position relative to the center.
//                        val touchOffset = change.position - center
//                        val currentAngle = Math.toDegrees(atan2(touchOffset.y, touchOffset.x).toDouble()).toFloat()
//                        previousAngle?.let { prev ->
//                            var delta = currentAngle - prev
//                            // Normalize the difference to account for crossing the 180°/–180° boundary.
//                            if (delta > 180f) delta -= 360f
//                            if (delta < -180f) delta += 360f
//                            // Update the total angle and constrain it between -360° and 360°.
//                            totalAngle = (totalAngle + delta).coerceIn(-360f, 360f)
//                            onAngleChanged(totalAngle)
//                        }
//                        previousAngle = currentAngle
//                        change.consumeAllChanges()
//                    },
//                    onDragEnd = {
//                        // Reset the previous angle when the drag ends.
//                        previousAngle = null
//                    },
//                    onDragCancel = {
//                        previousAngle = null
//                    }
//                )
//            }
//    ) {
//        // Draw the knob using a Canvas.
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            // Draw the outer circular track.
//            drawCircle(
//                color = Color.LightGray,
//                style = Stroke(width = 10f)
//            )
//            val radius = size.minDimension / 2
//            // Adjust the angle so that 0° is at the top.
//            val angleInRadians = Math.toRadians(totalAngle.toDouble()) - Math.PI / 2
//            val indicatorLength = radius * 0.8f
//            val indicatorEnd = Offset(
//                x = center.x + indicatorLength * cos(angleInRadians).toFloat(),
//                y = center.y + indicatorLength * sin(angleInRadians).toFloat()
//            )
//            // Draw a line that indicates the current angle.
//            drawLine(
//                color = Color.Red,
//                start = center,
//                end = indicatorEnd,
//                strokeWidth = 8f
//            )
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun AngleKnobPreview() {
//    // Maintain the angle state to be displayed.
//    var angle by remember { mutableStateOf(0f) }
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.Center
//    ) {
//        // Create a 200.dp x 200.dp knob
//        AngleKnob(
//            modifier = Modifier.size(200.dp),
//            initialAngle = angle
//        ) { newAngle ->
//            angle = newAngle
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//        Text(text = "Angle: ${angle.toInt()}°", fontSize = 24.sp)
//    }
//}
