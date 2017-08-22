package com.reedcwilson.personal_assistant.data

import android.arch.persistence.room.*
import io.reactivex.Flowable

@Dao
interface MessageDao {
    @Query("select * from message")
    fun getAll(): Flowable<List<Message>>

    @Query("select * from message where id = :id")
    fun findById(id: Long): Message

    @Insert
    fun insert(message: Message)

    @Update
    fun update(message: Message)

    @Delete
    fun delete(message: Message)
}