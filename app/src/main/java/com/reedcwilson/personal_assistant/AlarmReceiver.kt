package com.reedcwilson.personal_assistant

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.support.v4.app.NotificationCompat


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val myIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivities(context, 1, arrayOf(myIntent), 0)
        val icon = R.drawable.ic_launcher_background
        val time = System.currentTimeMillis()
        val not = NotificationCompat.Builder(context, "personal-assistant")
                .setSmallIcon(icon)
                .setContentTitle("Test")
                .setContentText("Whoa! Really cool!")
                .setWhen(time)
                .addAction(icon, "Send", pendingIntent)
                .addAction(icon, "Skip", pendingIntent)
                .build()
        not.flags = not.flags or Notification.FLAG_NO_CLEAR
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, not)
    }
}
