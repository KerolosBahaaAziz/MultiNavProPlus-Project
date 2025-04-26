//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.gestures.detectDragGestures
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.size
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableFloatStateOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.graphics.rotate
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import kotlin.math.PI
//import kotlin.math.atan2
//import kotlin.math.roundToInt
//
//@Composable
//fun AngleKnob(
//    modifier: Modifier = Modifier,
//    onAngleChange: (Float) -> Unit = {},
//    initialAngle: Float = 0f,
//    minAngle: Float = -360f,
//    maxAngle: Float = 360f
//) {
//    var currentAngle by remember { mutableFloatStateOf(initialAngle) }
//    var center by remember { mutableStateOf(Offset.Zero) }
//    var initialTouchAngle by remember { mutableFloatStateOf(0f) }
//
//    Box(
//        modifier = modifier
//            .pointerInput(Unit) {
//                detectDragGestures(
//                    onDragStart = { offset ->
//                        // Convert to proper Offset type
//                        val centerOffset = Offset(size.width / 2f, size.height / 2f)
//                        center = centerOffset
//                        // Convert drag start position to floating-point coordinates
//                        val touchOffset = Offset(offset.x, offset.y) - centerOffset
//                        val angle = (-atan2(touchOffset.y, touchOffset.x) * (180 / PI).toFloat() + 90)
//                        initialTouchAngle = (angle + 360) % 360
//                    },
//                    onDrag = { change, _ ->
//                        // Convert drag position to proper Offset type
//                        val currentPosition = Offset(
//                            change.position.x,
//                            change.position.y
//                        )
//                        val touchOffset = currentPosition - center
//                        val newAngle = (-atan2(touchOffset.y, touchOffset.x) * (180 / PI).toFloat() + 90
//                        val normalizedAngle = (newAngle + 360) % 360
//                        val angleDelta = (normalizedAngle - initialTouchAngle + 180) % 360 - 180
//
//                        currentAngle = (currentAngle + angleDelta)
//                            .coerceIn(minAngle, maxAngle)
//
//                        initialTouchAngle = normalizedAngle
//                        onAngleChange(currentAngle)
//                        change.consume()
//                    }
//                )
//            }
//    ) {
//        val strokeWidth = with(LocalDensity.current) { 4.dp.toPx() }
//        val markerLength = with(LocalDensity.current) { 24.dp.toPx() }
//
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            // Draw knob background
//            drawCircle(
//                color = Color.Gray,
//                radius = size.minDimension / 2f - 8.dp.toPx(),
//                style = Stroke(strokeWidth)
//
//                        // Draw rotating indicator
//                        rotate(currentAngle % 360) {
//                    drawLine(
//                        color = Color.White,
//                        start = center,
//                        end = center.copy(y = center.y - markerLength),
//                        strokeWidth = strokeWidth
//                    )
//                }
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun AngleKnobPreview() {
//    var angle by remember { mutableFloatStateOf(0f) }
//
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = Modifier.fillMaxSize()
//    ) {
//        AngleKnob(
//            modifier = Modifier.size(200.dp),
//            onAngleChange = { angle = it },
//            minAngle = -360f,
//            maxAngle = 360f
//        )
//        Text(text = "Angle: ${(angle * 10).roundToInt() / 10f}°")
//    }
//}