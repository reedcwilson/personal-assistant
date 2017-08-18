package com.reedcwilson.personal_assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.reedcwilson.personal_assistant.email.EmailClient
import com.reedcwilson.personal_assistant.email.EmailMessage


class MainActivity : AppCompatActivity() {

    private fun requestPermission(permission: String, btn: Button, resultNum: Int) {
        btn.setOnClickListener {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), resultNum)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission(Manifest.permission.SEND_SMS, findViewById<Button>(R.id.sendBtn), 1)
        requestPermission(Manifest.permission.CALL_PHONE, findViewById<Button>(R.id.callBtn), 2)
        requestPermission(Manifest.permission.INTERNET, findViewById<Button>(R.id.sendBtn), 3)
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
        }
        catch (e: Throwable) {
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
