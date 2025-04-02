package com.example.bdwisher

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    private val NOTIFICATION_CHANNEL_ID = "birthday_channel"
    private val NOTIFICATION_REQUEST_CODE = 1001
    public val WHATSAPP_REQUEST_CODE = 2001

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Birthday Reminders"
            val descriptionText = "Channel for birthday reminder notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getFlagFromFirestore(callback: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val documentId = context.getString(R.string.document)

        val flagList = arrayListOf<String>()

        db.collection("flags").document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    for (i in 1..5) {
                        val flag = document.getString("flag$i") ?: "N/A"
                        flagList.add(flag)
                    }
                    callback(flagList.getOrElse(1) { "N/A" })
                } else {
                    Log.e("Firestore", "Document does not exist!")
                    callback("N/A")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching flags: ${exception.message}")
                callback("N/A")
            }
    }

    fun getTime(): Long {
        return TimeUnit.MINUTES.toMillis(241)
    }

    fun scheduleBirthdayNotification(birthday: Birthday) {
        try {
            if (wasNotificationShown(birthday)) {
                Log.d("NotificationScheduler", "Notification already shown for ${birthday.name} this year. Skipping...")
                getFlagFromFirestore { firstFlag ->
                    Log.d("NotificationScheduler", "Flag value: $firstFlag")
                }
                return
            }

            val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
            val birthdayDate = dateFormat.parse(birthday.date) ?: return

            val birthdayCal = Calendar.getInstance()
            val currentCal = Calendar.getInstance()

            birthdayCal.time = birthdayDate
            birthdayCal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR))
            birthdayCal.set(Calendar.HOUR_OF_DAY, 0)
            birthdayCal.set(Calendar.MINUTE, 0)
            birthdayCal.set(Calendar.SECOND, 0)

            var modifiedMillis = getTime()
            var notificationTime = birthdayCal.timeInMillis - modifiedMillis

            if (notificationTime <= System.currentTimeMillis()) {
                birthdayCal.add(Calendar.YEAR, 1)
            }

            notificationTime = birthdayCal.timeInMillis - modifiedMillis

            val notificationIntent = Intent(context, BirthdayNotificationReceiver::class.java).apply {
                putExtra("name", birthday.name)
                putExtra("id", birthday.id)
                putExtra("date", birthday.date)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, NOTIFICATION_REQUEST_CODE + birthday.id, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)
            }

            Log.d("NotificationScheduler", "Scheduled notification for ${birthday.name} at ${Date(notificationTime)}")

        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Error scheduling notification: ${e.message}")
        }
    }

    private fun wasNotificationShown(birthday: Birthday): Boolean {
        val prefs = context.getSharedPreferences("BirthdayPrefs", Context.MODE_PRIVATE)
        val lastNotifiedYear = prefs.getInt("notified_${birthday.id}", 0)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return lastNotifiedYear == currentYear
    }

    fun scheduleSmsMessage(birthday: Birthday) {
        try {
            if (wasWhatsappMessageSent(birthday)) {
                Log.d("NotificationScheduler", "SMS already sent for ${birthday.name} this year. Skipping...")
                return
            }

            val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
            val birthdayDate = dateFormat.parse(birthday.date) ?: return

            val birthdayCal = Calendar.getInstance()
            val currentCal = Calendar.getInstance()

            birthdayCal.time = birthdayDate
            birthdayCal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR))
            birthdayCal.set(Calendar.HOUR_OF_DAY, 0)
            birthdayCal.set(Calendar.MINUTE, 0)
            birthdayCal.set(Calendar.SECOND, 0)

            var smsTime = birthdayCal.timeInMillis - TimeUnit.MINUTES.toMillis(149)

            if (smsTime <= System.currentTimeMillis()) {
                birthdayCal.add(Calendar.YEAR, 1)
            }

            smsTime = birthdayCal.timeInMillis - TimeUnit.MINUTES.toMillis(149)

            val smsIntent = Intent(context, WhatsappMessageReceiver::class.java).apply {
                putExtra("name", birthday.name)
                putExtra("whatsapp", birthday.whatsapp)
                putExtra("id", birthday.id)
                putExtra("wish", birthday.wish)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, WHATSAPP_REQUEST_CODE + birthday.id, smsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, smsTime, pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, smsTime, pendingIntent)
            }

            Log.d("NotificationScheduler", "Scheduled SMS for ${birthday.name} at ${Date(smsTime)}")

        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Error scheduling SMS: ${e.message}")
        }
    }

     fun wasWhatsappMessageSent(birthday: Birthday): Boolean {
        val prefs = context.getSharedPreferences("BirthdayPrefs", Context.MODE_PRIVATE)
        val lastWhatsMessagedYear = prefs.getInt("sms_sent_${birthday.id}", 0)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return lastWhatsMessagedYear == currentYear
    }
}
