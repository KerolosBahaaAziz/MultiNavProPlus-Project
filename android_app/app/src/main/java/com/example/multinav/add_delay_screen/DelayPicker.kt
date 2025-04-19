package com.example.multinav.add_delay_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DelayPicker(
    label: String,
    values: List<Int>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label,)
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