package com.example.widgets

import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.multinav.R
import kotlin.math.PI
import kotlin.math.atan2

@Composable
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

fun AngleKnob(

    modifier: Modifier,
    onValueChange: (Int) -> Unit={},
    step: Int = 1,
    range :IntRange = -360..360,
    ) {
    var rotation by remember { mutableStateOf(1) }
    var touchX = remember { mutableStateOf(0) }
    var touchY = remember { mutableStateOf(0) }
    var centerX by remember { mutableFloatStateOf(0f) }
    var centerY by remember { mutableFloatStateOf(0f) }
    Image(
        painter = painterResource( R.drawable.angle_knob),
        contentDescription = "Angle Knob",
        modifier = modifier.fillMaxSize()
            .onGloballyPositioned {
                val windowBounds = it.boundsInWindow()
                centerX =windowBounds.size.width/2f
                centerY = windowBounds.size.height/2f
            }
            .pointerInteropFilter { event ->
                val touchX = event.x
                val touchY = event.y
                val angle = -atan2(centerX - touchX, centerY - touchY) * (180f / PI).toFloat()
                when (event.action) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE -> {
                        rotation = angle.toInt()
                        val percent = ((rotation + 360f) % 360f) / 360f
                        onValueChange(percent.toInt())
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        rotation = 0
                        onValueChange(0)
                        true
                    }
                    else -> false
            }
            }
            .rotate(rotation.toFloat())
    )
}

@Preview
@Composable
private fun AngleKnobPrev() {


}