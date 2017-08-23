package com.reedcwilson.personal_assistant

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.ActivityCompat
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.*
import com.onegravity.contactpicker.contact.Contact
import com.onegravity.contactpicker.contact.ContactDescription
import com.onegravity.contactpicker.contact.ContactSortOrder
import com.onegravity.contactpicker.core.ContactPickerActivity
import com.onegravity.contactpicker.group.Group
import com.onegravity.contactpicker.picture.ContactPictureType
import com.reedcwilson.personal_assistant.data.Message
import com.reedcwilson.personal_assistant.email.EmailClient
import com.reedcwilson.personal_assistant.email.EmailMessage
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog


class MainActivity : Activity(), TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    lateinit var adapter: ArrayAdapter<Message>
    val PICK_CONTACTS_REQUEST_CODE = 1
    val SMS_PERMISSION_CODE = 1
    val PHONE_PERMISSION_CODE = 2
    val INTERNET_PERMISSION_CODE = 3
    val TAG: String = MainActivity::class.java.simpleName

    private fun requestPermission(permission: String, btn: Button, resultNum: Int) {
        btn.setOnClickListener {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), resultNum)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        requestPermission(Manifest.permission.SEND_SMS, sendBtn, SMS_PERMISSION_CODE)
//        requestPermission(Manifest.permission.CALL_PHONE, callBtn, PHONE_PERMISSION_CODE)
//        requestPermission(Manifest.permission.INTERNET, sendBtn, INTERNET_PERMISSION_CODE)

        adapter = ArrayAdapter<Message>(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter
        registerGetAllMessageListener()
    }

    override fun onDateSet(view: DatePickerDialog?, year: Int, month: Int, day: Int) {
        Toast.makeText(this, "you picked the following date: $year/$month/$day", Toast.LENGTH_SHORT).show()
    }

    override fun onTimeSet(view: TimePickerDialog?, hour: Int, minute: Int, second: Int) {
        Toast.makeText(this, "you picked the following date: $hour:$minute", Toast.LENGTH_SHORT).show()
    }


    fun add(content: String, type: String) {
        val message = Message(0, type, content)
        Single.fromCallable { MyApp.database?.messageDao()?.insert(message) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { id ->
                    Log.i(TAG, id.toString())
                }
    }

    fun selectDate(view: View) {
        val now = Calendar.getInstance()
        val pickerDialog = DatePickerDialog.newInstance(
                this,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH))
        pickerDialog.show(fragmentManager, "Datepickerdialog")
    }

    fun selectTime(view: View) {
        val now = Calendar.getInstance()
        val pickerDialog = TimePickerDialog.newInstance(
                this,
                now.get(Calendar.HOUR),
                now.get(Calendar.MINUTE),
                true)
        pickerDialog.show(fragmentManager, "Timepickerdialog")
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
                }
    }

    fun selectContact(view: View) {
        val intent = Intent(this, ContactPickerActivity::class.java)
//                .putExtra(ContactPickerActivity.EXTRA_THEME, if (darkTheme) R.style.Theme_Dark else R.style.Theme_Light)
                .putExtra(ContactPickerActivity.EXTRA_CONTACT_BADGE_TYPE, ContactPictureType.ROUND.name)
                .putExtra(ContactPickerActivity.EXTRA_SHOW_CHECK_ALL, true)
                .putExtra(ContactPickerActivity.EXTRA_CONTACT_DESCRIPTION, ContactDescription.ADDRESS.name)
                .putExtra(ContactPickerActivity.EXTRA_CONTACT_DESCRIPTION_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                .putExtra(ContactPickerActivity.EXTRA_CONTACT_SORT_ORDER, ContactSortOrder.AUTOMATIC.name)
        startActivityForResult(intent, PICK_CONTACTS_REQUEST_CODE)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACTS_REQUEST_CODE && resultCode == RESULT_OK && data!= null && data.hasExtra(ContactPickerActivity.RESULT_CONTACT_DATA)) {
            Log.d(TAG, "Response: " + data.toString())
            val contacts: List<*> = data.getSerializableExtra(ContactPickerActivity.RESULT_CONTACT_DATA) as List<*>
            var names = ""
            for (c in contacts) {
                val contact = c as Contact
                names = names + "," + contact.displayName
            }
            val groups: List<*> = data.getSerializableExtra(ContactPickerActivity.RESULT_GROUP_DATA) as List<*>
            for (g in groups) {
                val group = g as Group
            }
            contactName.setText(names, TextView.BufferType.EDITABLE)
        }
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
            Log.e(TAG, e.message, e)
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
