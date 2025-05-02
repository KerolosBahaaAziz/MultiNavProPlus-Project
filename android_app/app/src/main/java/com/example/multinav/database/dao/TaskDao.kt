package com.example.multinav.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.multinav.database.entities.Task

@Dao
interface TaskDao {

    @Query("SELECT * From Task WHERE userUid = :userId")
    fun getALLTasksByUserId(userId :String): List<Task>

    @Delete
    fun deleteTask(task : Task)

    @Insert
    fun addTask(task: Task)
}