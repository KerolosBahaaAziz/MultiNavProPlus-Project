package com.example.multinav.tasks_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multinav.database.MyDatabase
import com.example.multinav.database.entities.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TasksListViewModel: ViewModel() {


    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    val  taskDoa = MyDatabase.getInstance().getTaskDao()
    fun loadTasks(userId: String) {
        viewModelScope.launch {
            _tasks.value =  taskDoa.getALLTasksByUserId(userId)
        }
    }
    fun deleteTask(task: Task) {
        viewModelScope.launch {
              taskDoa.deleteTask(task)
            _tasks.value = _tasks.value.filter { it.taskId != task.taskId }
        }
    }



}