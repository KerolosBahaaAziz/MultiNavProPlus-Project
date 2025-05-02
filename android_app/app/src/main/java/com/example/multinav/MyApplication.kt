package com.example.multinav

import android.app.Application
import com.example.multinav.database.MyDatabase

class MyApplication :Application() {
    override fun onCreate() {
        super.onCreate()
        MyDatabase.initDatabase(this)
    }
}