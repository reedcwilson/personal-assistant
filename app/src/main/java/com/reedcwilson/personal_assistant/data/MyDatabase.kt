package com.reedcwilson.personal_assistant.data

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = arrayOf(Message::class), version = 1, exportSchema = false)
abstract class MyDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}