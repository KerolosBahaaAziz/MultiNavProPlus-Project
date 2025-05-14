package com.example.multinav.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class Task(

    @ColumnInfo
    val userUid : String,

    @ColumnInfo
    val taskTitle : String,

    @ColumnInfo
    val taskOn : Boolean? = false,

    @ColumnInfo
    val actions: List<String>,

    @PrimaryKey(autoGenerate = true)
    val taskId : Int,

    )
