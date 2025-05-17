package com.example.multinav.tasks_list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.multinav.Screen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun TaskSListScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: TasksListViewModel = viewModel()
) {
    val user = FirebaseAuth.getInstance().currentUser
    val userId = user?.uid
    viewModel.loadTasks(userId.toString())
    val tasks = viewModel.tasks.collectAsState().value


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
                IconButton( onClick = {
                    navController.navigate(Screen.JoyStick.route)
                }) {
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
            if (tasks.isEmpty()){
                Text(
                    text = "No tasks available",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .weight(1f)
                )

            }
            else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(tasks) { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.taskTitle,
                                color = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        navController.navigate(
                                            Screen.TaskActions.createRoute(
                                                taskTitle = task.taskTitle,
                                                taskId = task.taskId)
                                        )
                                    }
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            IconButton(
                                onClick = {

                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send task",
                                    modifier =Modifier.size(25.dp),
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteTask(task)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete task",
                                    modifier =Modifier.size(25.dp),
                                    tint = Color.White
                                )
                            }
                        }
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
                    onClick = {
                        navController.navigate(Screen.ActionsAndDelays.route)
                    },
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
    TaskSListScreen(
        navController = rememberNavController(),

    )
}