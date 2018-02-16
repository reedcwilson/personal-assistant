package com.reedcwilson.personal_assistant

import android.app.IntentService
import android.app.NotificationManager
import android.content.Intent
import android.util.Log
import com.reedcwilson.personal_assistant.email.EmailClient
import com.reedcwilson.personal_assistant.email.EmailMessage

val TAG: String = MessageService::class.java.simpleName

class MessageService : IntentService(TAG) {

    override fun onHandleIntent(p0: Intent?) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        Log.d(TAG, "is this even working!!!!!")
        notificationManager.cancel(1)
        sendEmail()
    }

    private fun sendEmail() {
        val from: String = "reedcwilson@gmail.com"
        val pwd: String = "pwd"
        EmailClient().execute(EmailMessage(
                from,
                pwd,
                "reedcwilson@gmail.com",
                "test",
                "this is a test",
                listOf()
        ))
    }
}
