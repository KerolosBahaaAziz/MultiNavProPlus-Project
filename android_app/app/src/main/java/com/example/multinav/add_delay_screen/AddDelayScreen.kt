package com.example.multinav.add_delay_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetDelayScreen(modifier: Modifier = Modifier ,viewModel: SetDelayViewModel = viewModel()) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black),
                title = {
                    Text(
                        text = "Set Delay",
                        maxLines = 1,
                        fontSize = 24.sp,
                        overflow = TextOverflow.Ellipsis
                    )

                },
                navigationIcon = {


                },

                )
        }
    ){
        innerPadding->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .background(Color.White)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly

            ){
                DelayPicker(
                    label = "Seconds",
                    values = (0..59).toList(), // Example range
                    selectedValue = viewModel.selectedSeconds.value,
                    onValueChange = { viewModel.selectedSeconds.value = it }
                )
                DelayPicker(
                    label = "Milliseconds",
                    values = (0..999).toList(), // Example range
                    selectedValue = viewModel.selectedMilliseconds.value,
                    onValueChange = {viewModel.selectedMilliseconds.value= it }
                )

            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Selected delay: ${viewModel.selectedSeconds.value}.${String.format("%02d", viewModel.selectedMilliseconds.value)} seconds",

                )
        }

    }
}