package com.example.desgin.actions_delays_screen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.desgin.constants.Modes
import com.example.widgets.CircleIconButton
import com.example.widgets.CircleToggleButton
import com.example.widgets.CustomTextField
import com.example.widgets.FiledButton
import com.example.widgets.RadioButtonMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsAndDelaysScreen(
    modifier: Modifier = Modifier,
    viewModel: ActionsAndDelaysViewModel = viewModel()){
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
                        text = "Actions & Delays",
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
        Column(modifier =Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                CustomTextField(
                    placeHolder = "Add Task",
                    textFiledValue = viewModel.textFiled,)
            }

            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                Column (
                    verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally){
                CircleIconButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = "",
                    onCircleButtonClick = {},
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row{
                    CircleIconButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDescription = "",
                        onCircleButtonClick = {},
                    )
                    Spacer(modifier = Modifier.width(64.dp))
                    CircleIconButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDescription = "",
                        onCircleButtonClick = {},
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                CircleIconButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = "",
                    onCircleButtonClick = {},
                )

            }
            }
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                Column (
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally){
                    CircleIconButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDescription = "",
                        onCircleButtonClick = {},
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row{
                        CircleIconButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            contentDescription = "",
                            onCircleButtonClick = {},
                        )
                        Spacer(modifier = Modifier.width(64.dp))
                        CircleIconButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            contentDescription = "",
                            onCircleButtonClick = {},
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    CircleIconButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDescription = "",
                        onCircleButtonClick = {},
                    )

                }
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                CircleToggleButton(
                    buttonName = "A",
                    isToggled = viewModel.isToggleButtonA,
                    onButtonClick = {
                            toggled ->
                        Log.e("ToggleButton A","${toggled}+A") }
                )

                CircleToggleButton(
                    buttonName = "B",
                    isToggled = viewModel.isToggleButtonB,
                    onButtonClick = {
                            toggled ->
                        Log.e("ToggleButton B","${toggled}B")
                    })

                CircleToggleButton(
                    buttonName = "C",
                    isToggled = viewModel.isToggleButtonC,
                    onButtonClick = {
                            toggled ->
                        Log.e("ToggleButton C","${toggled}C")
                    })

                CircleToggleButton(buttonName = "D",
                    isToggled = viewModel.isToggleButtonD,
                    onButtonClick = {
                            toggled ->
                        Log.e("ToggleButton D","${toggled}D")
                    })
            }
            Row (
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ){
                RadioButtonMode(
                    selectedModeState = viewModel.selectedMode,
                    modeName = Modes.MODE_ONE)

                RadioButtonMode(
                    selectedModeState = viewModel.selectedMode,
                    modeName = Modes.MODE_TWO)

                RadioButtonMode(
                    selectedModeState = viewModel.selectedMode,
                    modeName = Modes.MODE_THREE)
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                FiledButton(onClick = {}, buttonName = "Add Action")
                Spacer(modifier = Modifier.width(16.dp))
                FiledButton(onClick = {}, buttonName = "Add Delay")

            }
            Text(
                text = "History",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold)



        }
    }
}

@Preview
@Composable
private fun ActionsAndDelaysScreenPrev() {
    ActionsAndDelaysScreen()
    
}