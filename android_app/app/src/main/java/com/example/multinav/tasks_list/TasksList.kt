package com.example.multinav.tasks_list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TaskSList(modifier: Modifier = Modifier) {
    val actions = listOf(
        "action1",
        "action2",
        "action3",
        "action4",
        "action5",
        "action6",
        "action7",
        "action8",
        "action9",
        "action10",
        "action10",
        "action10",
        "action10",
        "action10",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF6A1B9A), Color(0xFFE91E63))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // Top Bar (Back button - you can customize this)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton( onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back"
                        )
                }
                Text(
                    text = "Back",
                )
                
                Spacer(modifier = Modifier.weight(1f)) // To push back button to the left
            }

            // List of Actions
            LazyColumn(
                modifier = Modifier.weight(1f) // Take up remaining vertical space
            ) {
                items(actions) { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = action,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))

                        var checkedState = remember { mutableStateOf(false) }
                        Switch(
                            checked = checkedState.value,
                            onCheckedChange = { checkedState.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color.LightGray,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }
                }
            }

            // Bottom Floating Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = { /* Handle FAB click */ },
                    containerColor = Color.White,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Filled.Add, "Add new item")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TaskSList()
}