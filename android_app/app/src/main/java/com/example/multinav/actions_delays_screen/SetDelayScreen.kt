package com.example.multinav.actions_delays_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.desgin.actions_delays_screen.ActionsAndDelaysViewModel

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetDelayScreen(
    modifier: Modifier = Modifier,
    viewModel: ActionsAndDelaysViewModel = viewModel(),
    navController: NavController = rememberNavController()
) {
    // Observe ViewModel state properties
    val hours by viewModel.selectedHours
    val minutes by viewModel.selectedMinutes
    val seconds by viewModel.selectedSeconds
    val milliseconds by viewModel.selectedMilliseconds

    // Calculate total milliseconds using derivedStateOf to ensure recomposition
    val totalMilliseconds by derivedStateOf {
        (hours * 3600000L) +  // Hours to milliseconds
                (minutes * 60000L) +   // Minutes to milliseconds
                (seconds * 1000L) +    // Seconds to milliseconds
                milliseconds           // Add milliseconds
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 18.sp,
                    modifier = Modifier.clickable {
                        viewModel.resetDelayPickerValues()
                        navController.popBackStack()
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Add",
                    fontSize = 18.sp,
                    modifier = Modifier.clickable {
                        viewModel.addDelayToHistory(totalMilliseconds)
                        viewModel.resetDelayPickerValues()
                        navController.popBackStack()
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DelayPicker(
                    label = "Hours",
                    values = (0..23).toList(),
                    selectedValue = hours,
                    onValueChange = { viewModel.selectedHours.value = it }
                )
                DelayPicker(
                    label = "Minutes",
                    values = (0..59).toList(),
                    selectedValue = minutes,
                    onValueChange = { viewModel.selectedMinutes.value = it }
                )
                DelayPicker(
                    label = "Seconds",
                    values = (0..59).toList(),
                    selectedValue = seconds,
                    onValueChange = { viewModel.selectedSeconds.value = it }
                )
                DelayPicker(
                    label = "Milliseconds",
                    values = (0..999).toList(),
                    selectedValue = milliseconds,
                    onValueChange = { viewModel.selectedMilliseconds.value = it }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Selected delay: $totalMilliseconds milliseconds",
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun DelayPicker(
    label: String,
    values: List<Int>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 100.dp)
        ) {
            items(values) { value ->
                val isSelected = value == selectedValue
                Text(
                    text = value.toString(),
                    modifier = Modifier
                        .clickable { onValueChange(value) }
                        .padding(vertical = 8.dp)
                        .then(
                            if (isSelected) {
                                Modifier.background(
                                    color = Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(8.dp)
                                ).padding(horizontal = 16.dp)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}