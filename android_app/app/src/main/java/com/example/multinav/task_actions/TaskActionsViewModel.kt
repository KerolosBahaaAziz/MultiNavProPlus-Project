package com.example.multinav.task_actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multinav.database.MyDatabase
import com.example.multinav.database.entities.Task
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskActionsViewModel : ViewModel() {

    private val taskDao = MyDatabase.getInstance().getTaskDao()

    private val _task = MutableStateFlow<Task?>(null)
    val task: StateFlow<Task?> = _task.asStateFlow()

    fun getTaskById(taskId: Int)  {
        viewModelScope.launch {
            _task.value = taskDao.getTaskById(taskId)

        }
    }
}