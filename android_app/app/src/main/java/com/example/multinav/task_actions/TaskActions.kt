package com.example.multinav.task_actions

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.multinav.Screen

@Composable
fun TaskActionsScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    taskTitle: String,
    taskId: Int,
    viewModel: TaskActionsViewModel = viewModel()
    ) {
    viewModel.getTaskById(taskId)
    val task = viewModel.task.collectAsState().value
    Box(
        modifier = Modifier
            .fillMaxSize()

    ){
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton( onClick = {
                    navController.navigate(Screen.TasksList.route)
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Back",
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = taskTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.weight(1f))

            }

            Text(
                text = "History",
                fontSize = 22.sp
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                     // Adjust height as needed
            ) {

                items(task?.actions ?: emptyList()) { actions ->
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
                            text = actions,
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

//@Preview(showBackground = true)
//@Composable
//private fun TaskActionsScreenPreview() {
//    TaskActionsScreen(
//        navController = rememberNavController(),
//    )
//}