package com.example.demo

import android.app.Application
import androidx.room.Room
import com.example.demo.data.AppDatabase

class App : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }
} 