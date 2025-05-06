package com.example.desgin.actions_delays_screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.desgin.constants.Modes
import com.example.multinav.R
import com.example.multinav.ui.theme.violetPurple
import com.example.widgets.CircleToggleButton
import com.example.widgets.CustomTextField
import com.example.widgets.RadioButtonMode
import com.google.firebase.auth.FirebaseAuth


@Composable
fun CircleIconButton(
    icon: @Composable () -> Unit, // Changed to accept a composable lambda
    contentDescription: String,
    onCircleButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onCircleButtonClick,
        modifier = modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
        colors = IconButtonColors(
            containerColor = violetPurple,
            contentColor = violetPurple,
            disabledContainerColor = violetPurple,
            disabledContentColor = violetPurple
        )
    ){
        icon()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsAndDelaysScreen(
    modifier: Modifier = Modifier,
    viewModel: ActionsAndDelaysViewModel = viewModel(),
    navController: NavController = rememberNavController())
{
    val user = FirebaseAuth.getInstance().currentUser
    val actionHistory = viewModel.actionHistory
    val textFieldValue by viewModel.textField
    val selectedAction by viewModel.selectedAction
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
                        onClick = {
                            navController.popBackStack()
                        }
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
                    textFiledValue = viewModel.textField,)
            }

            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircleIconButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Up",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        },
                        contentDescription = "Up",
                        onCircleButtonClick = {
                            viewModel.addActionToHistory("Up")
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        CircleIconButton(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Left",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            },
                            contentDescription = "Left",
                            onCircleButtonClick = {
                                viewModel.addActionToHistory("Left")
                            },
                        )
                        Spacer(modifier = Modifier.width(64.dp))
                        CircleIconButton(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Right",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            },
                            contentDescription = "Right",
                            onCircleButtonClick = {
                                viewModel.addActionToHistory("Right")
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CircleIconButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Down",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        },
                        contentDescription = "Down",
                        onCircleButtonClick = {
                            viewModel.addActionToHistory("Down")
                        },
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
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top: Triangle
                    CircleIconButton(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_triangle),
                                contentDescription = "Triangle",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        },
                        contentDescription = "Triangle",
                        onCircleButtonClick = {
                            viewModel.addActionToHistory("Triangle")
                        },

                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Middle: Square (left) and Circle (right)
                    Row {
                        CircleIconButton(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_square),
                                    contentDescription = "Square",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            },
                            contentDescription = "Square",
                            onCircleButtonClick = {
                                viewModel.addActionToHistory("Square")
                            },
                        )
                        Spacer(modifier = Modifier.width(64.dp))
                        CircleIconButton(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_circle),
                                    contentDescription = "Circle",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            },
                            contentDescription = "Circle",
                            onCircleButtonClick = {
                                viewModel.addActionToHistory("Circle")
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom: Cross
                    CircleIconButton(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_x),
                                contentDescription = "Cross",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        },
                        contentDescription = "Cross",
                        onCircleButtonClick = {
                            viewModel.addActionToHistory("Cross")
                        },
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
                        viewModel.addActionToHistoryToggleButtons("A")
                        Log.e("ToggleButton A","${toggled}+A") }
                )

                CircleToggleButton(
                    buttonName = "B",
                    isToggled = viewModel.isToggleButtonB,
                    onButtonClick = {
                            toggled ->
                        viewModel.addActionToHistoryToggleButtons("B")
                        Log.e("ToggleButton B","${toggled}B")
                    })

                CircleToggleButton(
                    buttonName = "C",
                    isToggled = viewModel.isToggleButtonC,
                    onButtonClick = {
                            toggled ->
                        viewModel.addActionToHistoryToggleButtons("C")
                        Log.e("ToggleButton C","${toggled}C")
                    })

                CircleToggleButton(buttonName = "D",
                    isToggled = viewModel.isToggleButtonD,
                    onButtonClick = {
                            toggled ->
                        viewModel.addActionToHistoryToggleButtons("D")
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
                    modeName = Modes.MODE_ONE,
                    onClick = { viewModel.updateModeInHistory(Modes.MODE_ONE) }
                )
                RadioButtonMode(
                    selectedModeState = viewModel.selectedMode,
                    modeName = Modes.MODE_TWO,
                    onClick = { viewModel.updateModeInHistory(Modes.MODE_TWO) }
                )
                RadioButtonMode(
                    selectedModeState = viewModel.selectedMode,
                    modeName = Modes.MODE_THREE,
                    onClick = { viewModel.updateModeInHistory(Modes.MODE_THREE) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),

                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = {
                        val taskTitle = textFieldValue.takeIf { textFieldValue.isNotBlank() } ?: "Default Task"
                        val userUid = user?.uid
                        viewModel.saveActionToDatabase(userUid !!, taskTitle)
                        navController.popBackStack()
                    },

                    enabled = selectedAction != null // Enable only if an action is selected
                ) {
                    Text("Add Task")
                }
                Button(
                    onClick = {
                        Log.d("ActionsAndDelays", "Add Delay button clicked")
                        navController.navigate("set_delay_screen")
                    },

                ) {
                    Text("Add Delay ")
                }

            }
            Text(
                text = "History",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // Adjust height as needed

            ) {
                Log.d("ActionsAndDelaysScreen", "Rendering LazyColumn with history: ${viewModel.actionHistory}")
                items(viewModel.actionHistory) { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .padding(4.dp)
                            .background(Color.LightGray, shape = MaterialTheme.shapes.large),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = action,
                            fontSize = 18.sp,
                            color = Color.Blue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }

                }
            }


        }
    }
}
 fun formatDelay(milliseconds: Long): String {
    val hours = milliseconds / 3600000
    val minutes = (milliseconds % 3600000) / 60000
    val seconds = (milliseconds % 60000) / 1000
    val millis = milliseconds % 1000
    return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
}

@Preview
@Composable
private fun ActionsAndDelaysScreenPrev() {
    ActionsAndDelaysScreen()

}