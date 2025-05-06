package com.example.multinav.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class Task(

    @ColumnInfo
    val buttonA: Char? = null,

    @ColumnInfo
    val buttonB: Char? = null,

    @ColumnInfo
    val buttonC: Char? = null ,

    @ColumnInfo
    val buttonD:Char? = null,

    @ColumnInfo
    val buttonUP: String? = null,

    @ColumnInfo
    val buttonDown: String? = null,

    @ColumnInfo
    val buttonRight: String? = null,

    @ColumnInfo
    val buttonLeft:String? = null,

    @ColumnInfo
    val buttonTriangle: String? = null,

    @ColumnInfo
    val buttonX: String? = null,

    @ColumnInfo
    val buttonCircle: String? = null,

    @ColumnInfo
    val buttonSquare:String? = null,


    @ColumnInfo
    val mode: String = "1",

    @ColumnInfo
    val delay : Long? =null,

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
