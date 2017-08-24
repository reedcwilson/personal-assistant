package com.reedcwilson.personal_assistant

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.telephony.SmsManager
import android.text.Layout
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
import java.text.SimpleDateFormat


class MainActivity : Activity(), TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    lateinit var adapter: ArrayAdapter<Message>
    lateinit var rootLayout: View
    val PICK_CONTACTS_REQUEST_CODE = 1
    val SMS_PERMISSION_CODE = 1
    val PHONE_PERMISSION_CODE = 2
    val INTERNET_PERMISSION_CODE = 3
    val TAG: String = MainActivity::class.java.simpleName

    var contact: Contact? = null
    var message: String? = null
    var date: Date? = null
    var time: Date? = null

    private fun requestPermission(permission: String, btn: Button, resultNum: Int) {
        btn.setOnClickListener {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), resultNum)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rootLayout = main_layout
//        requestPermission(Manifest.permission.SEND_SMS, sendBtn, SMS_PERMISSION_CODE)
//        requestPermission(Manifest.permission.CALL_PHONE, callBtn, PHONE_PERMISSION_CODE)
//        requestPermission(Manifest.permission.INTERNET, sendBtn, INTERNET_PERMISSION_CODE)
        requestPermission(Manifest.permission.READ_CONTACTS, selectContactBtn, 4)

        adapter = ArrayAdapter<Message>(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter
        registerGetAllMessageListener()
    }

    override fun onDateSet(view: DatePickerDialog?, year: Int, month: Int, day: Int) {
        date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("$year-$month-$day")
    }

    override fun onTimeSet(view: TimePickerDialog?, hour: Int, minute: Int, second: Int) {
        time = SimpleDateFormat("HH:mm", Locale.US).parse("$hour:$minute")
    }


    fun add(contact: Contact?, content: String?, type: String?, date: Date?) {
        var contactInfo = contact!!.getPhone(0)
        if (type == "Email") {
            contactInfo = contact.getEmail(0)
        }
        val message = Message(0, type!!, content!!, contactInfo, contact.displayName, date!!.time)
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

    fun createDate(d: Date, t: Date) : Date {
        val date = Calendar.getInstance()
        date.time = d
        val time = Calendar.getInstance()
        time.time = t
        val calendar = Calendar.getInstance()
        calendar.set(
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH),
                time.get(Calendar.HOUR_OF_DAY),
                time.get(Calendar.MINUTE)
        )
        return calendar.time
    }

    fun addMessage(view: View) {
        add(contact, messageTxt.text.toString(), messageTypes.selectedItem.toString(), createDate(date!!, time!!))
        Snackbar.make(rootLayout, "Message added", Snackbar.LENGTH_SHORT).show()
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

//    fun selectContact(view: View) {
    fun selectContact() {
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
        Snackbar.make(rootLayout, "Alarm Set", Snackbar.LENGTH_SHORT).show()
    }

    fun cancelAlarm(view: View) {
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val id = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getBroadcast(this, id, alarmIntent, 0)
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent)
        Snackbar.make(rootLayout, "Alarm Canceled", Snackbar.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACTS_REQUEST_CODE && resultCode == RESULT_OK && data!= null && data.hasExtra(ContactPickerActivity.RESULT_CONTACT_DATA)) {
            Log.d(TAG, "Response: " + data.toString())
            val contacts: List<*> = data.getSerializableExtra(ContactPickerActivity.RESULT_CONTACT_DATA) as List<*>
            var names = ""
            contact = contacts[0] as Contact
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
                    Snackbar.make(rootLayout, "Permission denied to send SMS", Snackbar.LENGTH_SHORT).show()
                }
                return
            }
            2 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    makePhoneCall("8017057567")
                } else {
                    Snackbar.make(rootLayout, "Permission denied to make phone call", Snackbar.LENGTH_SHORT).show()
                }
                return
            }
            3 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendEmail("reedcwilson@gmail.com", "test", "this is a test")
                } else {
                    Snackbar.make(rootLayout, "Permission denied to send email", Snackbar.LENGTH_SHORT).show()
                }
                return
            }
            4 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectContact()
                } else {
                    Snackbar.make(rootLayout, "Permission denied to send email", Snackbar.LENGTH_SHORT).show()
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
