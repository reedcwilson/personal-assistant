package com.reedcwilson.personal_assistant.data

import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.Database

@Database(entities = arrayOf(Message::class), version = 1, exportSchema = false)
abstract class MyDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}