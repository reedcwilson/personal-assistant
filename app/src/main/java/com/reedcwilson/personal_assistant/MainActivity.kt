package com.reedcwilson.personal_assistant

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import com.reedcwilson.personal_assistant.data.Message
import com.reedcwilson.personal_assistant.email.EmailClient
import com.reedcwilson.personal_assistant.email.EmailMessage
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : Activity() {

    lateinit var adapter: ArrayAdapter<Message>

    private fun requestPermission(permission: String, btn: Button, resultNum: Int) {
        btn.setOnClickListener {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), resultNum)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission(Manifest.permission.SEND_SMS, sendBtn, 1)
        requestPermission(Manifest.permission.CALL_PHONE, callBtn, 2)
        requestPermission(Manifest.permission.INTERNET, sendBtn, 3)

        adapter = ArrayAdapter<Message>(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter
        registerGetAllMessageListener()
    }

    fun add(content: String, type: String) {
        val message = Message(0, type, content)
        Single.fromCallable { MyApp.database?.messageDao()?.insert(message) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { id ->
                    Log.i("tag", id.toString())
                }
    }

    fun addMessage(view: View) {
        add(System.currentTimeMillis().toString(), "phone")
        Toast.makeText(this, "Message added", Toast.LENGTH_SHORT).show()
    }

    fun registerGetAllMessageListener() {
        MyApp.database?.messageDao()?.getAll()
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe { messages ->
                    adapter.clear()
                    adapter.addAll(messages)
                    msgNum.text = System.currentTimeMillis().toString()
                }
    }

    fun startAlarm(view: View) {
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val id = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getBroadcast(this, id, alarmIntent, 0)
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pendingIntent)
        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show()
    }

    fun cancelAlarm(view: View) {
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val id = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getBroadcast(this, id, alarmIntent, 0)
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent)
        Toast.makeText(this, "Alarm Canceled", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSms("8018229975", "this is a test")
                } else {
                    Toast.makeText(this@MainActivity, "Permission denied to send SMS", Toast.LENGTH_SHORT).show()
                }
                return
            }
            2 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    makePhoneCall("8017057567")
                } else {
                    Toast.makeText(this@MainActivity, "Permission denied to make phone call", Toast.LENGTH_SHORT).show()
                }
                return
            }
            3 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendEmail("reedcwilson@gmail.com", "test", "this is a test")
                } else {
                    Toast.makeText(this@MainActivity, "Permission denied to send email", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    private fun sendEmail(to: String, subject: String, message: String) {
        val from: String = "reedcwilson@gmail.com"
        val pwd: String = "pwd"
        EmailClient().execute(EmailMessage(
                from,
                pwd,
                to,
                subject,
                message,
                listOf()
        ))
    }

    private fun sendSms(number: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, message, null, null)
        } catch (e: Throwable) {
            Log.e("SendSMS", e.message, e)
        }
    }

    private fun makePhoneCall(number: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:" + number)

        val permission = "android.permission.CALL_PHONE"
        val res = checkCallingOrSelfPermission(permission)
        if (res == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent)
        }
    }
}
