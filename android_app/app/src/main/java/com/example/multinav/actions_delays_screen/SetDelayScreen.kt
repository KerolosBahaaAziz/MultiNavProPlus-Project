package com.example.multinav.actions_delays_screen

import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.desgin.actions_delays_screen.ActionsAndDelaysViewModel

@Composable
fun SetDelayScreen(
    modifier: Modifier = Modifier,
    viewModel: ActionsAndDelaysViewModel = viewModel(),
    navController: NavController = rememberNavController()
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,

        ){
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
                        navController.popBackStack()
                    }
                )

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Add",
                    fontSize = 18.sp,
                    modifier = Modifier.clickable {
                        viewModel.addDelayToHistory()
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
                    values = (0..23).toList(), // Range for hours
                    selectedValue = viewModel.selectedHours.value,
                    onValueChange = { viewModel.selectedHours.value = it }
                )
                DelayPicker(
                    label = "Minutes",
                    values = (0..59).toList(), // Range for minutes
                    selectedValue = viewModel.selectedMinutes.value,
                    onValueChange = { viewModel.selectedMinutes.value = it }
                )
                DelayPicker(
                    label = "Seconds",
                    values = (0..59).toList(), // Example range
                    selectedValue =viewModel.selectedSeconds.value,
                    onValueChange = { viewModel.selectedSeconds.value = it }
                )
                DelayPicker(
                    label = "Milliseconds",
                    values = (0..999).toList(), // Example range
                    selectedValue = viewModel.selectedMilliseconds.value,
                    onValueChange = { viewModel.selectedMilliseconds.value = it }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Selected delay: ${String.format("%02d", viewModel.selectedHours.value)}:" +
                        "${String.format("%02d", viewModel.selectedMinutes.value)}:" +
                        "${String.format("%02d", viewModel.selectedSeconds.value)}." +
                        "${String.format("%03d", viewModel.selectedMilliseconds.value)}",
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
                .heightIn(max = 100.dp) // Limit the visible height
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
                                    color = Color(0xFFE0E0E0), // Light gray background
                                    shape = RoundedCornerShape(8.dp)
                                ).padding(horizontal = 16.dp) // Add padding around selected item
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}

