package com.example.ringer

import android.app.Application
import com.example.ringer.data.AppDatabase
import com.example.ringer.data.LockRepository

class RingerApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { LockRepository(database.appDao(), this) }

    companion object {
        lateinit var instance: RingerApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}