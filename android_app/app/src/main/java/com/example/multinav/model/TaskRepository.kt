package com.example.multinav.model

import com.example.multinav.database.dao.TaskDao
import com.example.multinav.database.entities.Task

// TaskRepository.kt
class TaskRepository(private val taskDao: TaskDao) {
    suspend fun getAllTasksByUserId(userId: String): List<Task> {
        return taskDao.getALLTasksByUserId(userId)
    }

    suspend fun addTask(task: Task) {
        taskDao.addTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun getTaskById(taskId: Int): Task? {
        return taskDao.getTaskById(taskId)
    }
}