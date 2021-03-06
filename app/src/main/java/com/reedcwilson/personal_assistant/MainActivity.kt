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
import android.provider.ContactsContract
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import com.onegravity.contactpicker.contact.Contact
import com.onegravity.contactpicker.contact.ContactDescription
import com.onegravity.contactpicker.contact.ContactSortOrder
import com.onegravity.contactpicker.core.ContactPickerActivity
import com.onegravity.contactpicker.group.Group
import com.onegravity.contactpicker.picture.ContactPictureType
import com.reedcwilson.personal_assistant.data.Message
import com.reedcwilson.personal_assistant.email.EmailClient
import com.reedcwilson.personal_assistant.email.EmailMessage
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : Activity(), TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    lateinit var adapter: ArrayAdapter<Message>
    lateinit var rootLayout: View
    val PICK_CONTACTS_REQUEST_CODE = 1
    val SMS_PERMISSION_CODE = 1
    val PHONE_PERMISSION_CODE = 2
    val INTERNET_PERMISSION_CODE = 3
    val CONTACTS_PERMISSION_CODE = 4
    val TAG: String = MainActivity::class.java.simpleName

    var contact: Contact? = null
    var message: String? = null
    var date: Date? = null
    var time: Date? = null

    fun checkPermissionDoOp(permission: String, resultNum: Int, func: () -> Unit) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), resultNum)
            return
        }
        func()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rootLayout = main_layout
//        checkPermissionDoOp(Manifest.permission.SEND_SMS, SMS_PERMISSION_CODE, { sendSms() })
//        checkPermissionDoOp(Manifest.permission.CALL_PHONE, PHONE_PERMISSION_CODE, { makePhoneCall() })
//        checkPermissionDoOp(Manifest.permission.INTERNET, INTERNET_PERMISSION_CODE, { sendEmail() })
        selectContactBtn.setOnClickListener {
            checkPermissionDoOp(Manifest.permission.READ_CONTACTS, CONTACTS_PERMISSION_CODE, { selectContacts() })
        }

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

    fun addMessage(view: View) {
        val d = createDate(date!!, time!!)
        add(contact, messageTxt.text.toString(), messageTypes.selectedItem.toString(), d)
        startAlarm(d)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACTS_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.hasExtra(ContactPickerActivity.RESULT_CONTACT_DATA)) {
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
            SMS_PERMISSION_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSms()
                } else {
                    Snackbar.make(rootLayout, "Permission denied to send SMS", Snackbar.LENGTH_SHORT).show()
                }
                return
            }
            PHONE_PERMISSION_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    makePhoneCall()
                } else {
                    Snackbar.make(rootLayout, "Permission denied to make phone call", Snackbar.LENGTH_SHORT).show()
                }
                return
            }
            INTERNET_PERMISSION_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendEmail()
                } else {
                    Snackbar.make(rootLayout, "Permission denied to send email", Snackbar.LENGTH_SHORT).show()
                }
                return
            }
            CONTACTS_PERMISSION_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectContacts()
                } else {
                    Snackbar.make(rootLayout, "Permission denied to send email", Snackbar.LENGTH_SHORT).show()
                }
                return
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun registerGetAllMessageListener() {
        MyApp.database?.messageDao()?.getAll()
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe { messages ->
                    adapter.clear()
                    adapter.addAll(messages)
                }
    }

    //    private fun selectContacts(view: View) {
    private fun selectContacts() {
        val intent = Intent(this, ContactPickerActivity::class.java)
//                .putExtra(ContactPickerActivity.EXTRA_THEME, if (darkTheme) R.style.Theme_Dark else R.style.Theme_Light)
                .putExtra(ContactPickerActivity.EXTRA_CONTACT_BADGE_TYPE, ContactPictureType.ROUND.name)
                .putExtra(ContactPickerActivity.EXTRA_SHOW_CHECK_ALL, true)
                .putExtra(ContactPickerActivity.EXTRA_CONTACT_DESCRIPTION, ContactDescription.ADDRESS.name)
                .putExtra(ContactPickerActivity.EXTRA_CONTACT_DESCRIPTION_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                .putExtra(ContactPickerActivity.EXTRA_CONTACT_SORT_ORDER, ContactSortOrder.AUTOMATIC.name)
        startActivityForResult(intent, PICK_CONTACTS_REQUEST_CODE)
    }

    fun setAlarm(view: View) {
        startAlarm(Date(System.currentTimeMillis() + 5000))
    }

    private fun startAlarm(d: Date) {
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val id = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getBroadcast(this, id, alarmIntent, 0)
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.set(AlarmManager.RTC_WAKEUP, d.time, pendingIntent)
        Snackbar.make(rootLayout, "Alarm Set", Snackbar.LENGTH_SHORT).show()
    }

    private fun cancelAlarm() {
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val id = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getBroadcast(this, id, alarmIntent, 0)
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent)
        Snackbar.make(rootLayout, "Alarm Canceled", Snackbar.LENGTH_SHORT).show()
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

    private fun sendSms() {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage("+8018229975", null, "this is a test", null, null)
        } catch (e: Throwable) {
            Log.e(TAG, e.message, e)
        }
    }

    private fun makePhoneCall() {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:" + "8017057567")

        val permission = "android.permission.CALL_PHONE"
        val res = checkCallingOrSelfPermission(permission)
        if (res == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent)
        }
    }

    private fun createDate(d: Date, t: Date): Date {
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
}
