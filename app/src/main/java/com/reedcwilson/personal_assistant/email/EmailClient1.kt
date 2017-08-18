package com.reedcwilson.personal_assistant.email

import android.os.AsyncTask
import android.util.Log

data class EmailMessage(
        val from: String,
        val pwd: String,
        val to: String,
        val subject: String,
        val message: String,
        val attachments: List<String>)

class EmailClient : AsyncTask<EmailMessage, Void, Void>() {
    override fun doInBackground(vararg messages: EmailMessage?): Void? {
        val message = messages[0]
        if (message != null) {
            try {
                val sender = GMailSender(message.from, message.pwd)
                sender.sendMail(message.subject,
                        message.message,
                        message.from,
                        message.to,
                        message.attachments)
            } catch (e: Exception) {
                Log.e("SendMail", e.message, e)
            }
        }
        return null
    }
}
