package com.reedcwilson.personal_assistant

import android.app.Application
import android.arch.persistence.room.Room
import com.reedcwilson.personal_assistant.data.MyDatabase

class MyApp : Application() {
    companion object {
        var database: MyDatabase? = null
    }

    override fun onCreate() {
        super.onCreate()
        MyApp.database =  Room.databaseBuilder(this, MyDatabase::class.java, "personal-assistant-db").build()
    }
}