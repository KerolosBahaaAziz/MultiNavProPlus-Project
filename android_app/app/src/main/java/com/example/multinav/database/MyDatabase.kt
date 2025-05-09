package com.example.multinav.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.multinav.database.dao.TaskDao
import com.example.multinav.database.entities.Task
import com.example.multinav.database.type_converter.Converters

@TypeConverters(Converters::class)
@Database(entities = [Task::class], version = 1)
abstract class MyDatabase : RoomDatabase() {

    abstract fun getTaskDao(): TaskDao

    companion object {
        private var database: MyDatabase? = null

        fun initDatabase(context: Context) {
            if (database == null) {
                database = Room.databaseBuilder(
                    context = context,
                    MyDatabase::class.java,
                    name = "TaskDatabase"
                ).build()
            }
        }

        fun getInstance(): MyDatabase {
            return database ?: throw IllegalStateException("Database not initialized")
        }
    }
}