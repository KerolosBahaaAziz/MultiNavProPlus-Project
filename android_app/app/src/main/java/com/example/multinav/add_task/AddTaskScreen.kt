package com.example.multinav.add_task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.widgets.FloatingButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(modifier: Modifier = Modifier) {
    val actions  = listOf(
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
        )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val topAppBarBackgroundColor = Color(0xFF6A1B9A)
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = topAppBarBackgroundColor,
                        titleContentColor = Color.White),
                    title = {
                        Text(
                            text = "Insert Task",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {}
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Arrow Back"
                            )
                        }

                    },

                    )
            }
        ){
            innerPadding->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF6A1B9A), Color(0xFFE91E63))
                        )
                    )
            ){
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(actions) { action ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = action, color = Color.White)
                                Spacer(modifier = Modifier.weight(1f))
                                val checkedState = remember { mutableStateOf(false) }
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ){
                        FloatingButton(onClick = {  })
                    }
                }
            }
        }
}

