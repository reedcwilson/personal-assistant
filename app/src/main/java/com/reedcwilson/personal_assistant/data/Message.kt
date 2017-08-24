package com.reedcwilson.personal_assistant.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "message")
data class Message(
        @PrimaryKey(autoGenerate = true)
        val id: Long,
        val type: String = "",
        val content: String = "",
        val contactInfo: String = "",
        val contactName: String = "",
        val timeMillis: Long? = null,
        val active: Boolean = false
)

