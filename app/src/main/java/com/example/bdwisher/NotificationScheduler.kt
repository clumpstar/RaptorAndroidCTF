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

object NotificationScheduler {
    private const val NOTIFICATION_CHANNEL_ID = "birthday_channel"
    private const val NOTIFICATION_REQUEST_CODE = 1001
    const val WHATSAPP_REQUEST_CODE = 2001
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Birthday Reminders"
            val descriptionText = "Channel for birthday reminder notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

//    fun scheduleBirthdayNotification(context: Context, birthday: Birthday) {
//        try {
//
//            if (wasNotificationShown(context, birthday)) {
//                Log.d("NotificationScheduler", "Notification already shown for ${birthday.name} this year. Skipping...")
//                return
//            }
//
//            // Create notification channel for Android 8.0+
//            createNotificationChannel(context)
//
//            // Parse the birthday date (assuming format is DD/MM)
//            val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
//            val birthdayDate = dateFormat.parse(birthday.date) ?: return
//
//            // Get calendar instance for the next occurrence of this birthday
//            val birthdayCal = Calendar.getInstance()
//            val currentCal = Calendar.getInstance()
//
//            // Set birthday time to the date with hours set to 00:00:00
//            birthdayCal.time = birthdayDate
//            birthdayCal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR))
//            birthdayCal.set(Calendar.HOUR_OF_DAY, 0)
//            birthdayCal.set(Calendar.MINUTE, 0)
//            birthdayCal.set(Calendar.SECOND, 0)
//
//            // If the birthday has already passed this year, schedule for next year
//            if (birthdayCal.before(currentCal)) {
//                birthdayCal.add(Calendar.YEAR, 1)
//            }
//
//            // Calculate time 5 hours before the birthday
////            val notificationTime = birthdayCal.timeInMillis - TimeUnit.HOURS.toMillis(5)
//            val notificationTime = birthdayCal.timeInMillis - TimeUnit.MINUTES.toMillis(85)
//
//            // Create intent for the notification
//            val notificationIntent = Intent(context, BirthdayNotificationReceiver::class.java)
//            notificationIntent.putExtra("name", birthday.name)
//            notificationIntent.putExtra("id", birthday.id)
//            notificationIntent.putExtra("date", birthday.date)
//
//            // Create pending intent
//            val pendingIntent = PendingIntent.getBroadcast(
//                context,
//                NOTIFICATION_REQUEST_CODE + birthday.id, // Unique request code for each birthday
//                notificationIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//
//            // Get alarm manager
//            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//            // Schedule the notification alarm
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                alarmManager.setExactAndAllowWhileIdle(
//                    AlarmManager.RTC_WAKEUP,
//                    notificationTime,
//                    pendingIntent
//                )
//            } else {
//                alarmManager.setExact(
//                    AlarmManager.RTC_WAKEUP,
//                    notificationTime,
//                    pendingIntent
//                )
//            }
//
//            Log.d("NotificationScheduler", "Scheduled notification for ${birthday.name} at ${Date(notificationTime)}")
//
//        } catch (e: Exception) {
//            Log.e("NotificationScheduler", "Error scheduling notification: ${e.message}")
//        }
//    }

    fun getFlagFromFirestore(context: Context, callback: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val documentId = context.getString(R.string.document)

        val flagList = arrayListOf<String>()

        db.collection("flags").document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    for (i in 1..5) { // Assuming flag1 to flag5
                        val flag = document.getString("flag$i") ?: "N/A"
                        flagList.add(flag)
                    }

                    // Return the first flag via callback
                    callback(flagList.getOrElse(1) { "N/A" })
                } else {
                    Log.e("Firestore", "Document does not exist!")
                    callback("N/A") // Return "N/A" if the document doesn't exist
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching flags: ${exception.message}")
                callback("N/A") // Return "N/A" if fetching fails
            }
    }


    fun getTime(): Long {
        return TimeUnit.MINUTES.toMillis(241)
    }


    fun scheduleBirthdayNotification(context: Context, birthday: Birthday) {
        try {

            if (wasNotificationShown(context, birthday)) {
                Log.d("NotificationScheduler", "Notification already shown for ${birthday.name} this year. Skipping...")
//                Log.d("NotificationScheduler", "ooouch!! the flag is misplaced ${getFlagFromFirestore(context)}")
                getFlagFromFirestore(context) { firstFlag ->
                    Log.d("NotificationScheduler", "ooouch!! the flag is misplaced $firstFlag")
                }
                return
            }

            createNotificationChannel(context)

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

            val notificationTime = birthdayCal.timeInMillis - modifiedMillis

            // Check if notificationTime is in the past
            if (notificationTime <= System.currentTimeMillis()) {
                birthdayCal.add(Calendar.YEAR, 1) // Move to next year
            }

            val adjustedNotificationTime = birthdayCal.timeInMillis - modifiedMillis

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
                    AlarmManager.RTC_WAKEUP, adjustedNotificationTime, pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, adjustedNotificationTime, pendingIntent)
            }

            Log.d("NotificationScheduler", "Scheduled notification for ${birthday.name} at ${Date(adjustedNotificationTime)}")

        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Error scheduling notification: ${e.message}")
        }
    }


    private fun wasNotificationShown(context: Context, birthday: Birthday): Boolean {
        val prefs = context.getSharedPreferences("BirthdayPrefs", Context.MODE_PRIVATE)
        val lastNotifiedYear = prefs.getInt("notified_${birthday.id}", 0)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return lastNotifiedYear == currentYear
    }

    private fun wasSmsMessageSent(context: Context, birthday: Birthday): Boolean {
        val prefs = context.getSharedPreferences("BirthdayPrefs", Context.MODE_PRIVATE)
        val lastWhatsMessagedYear = prefs.getInt("sms_sent_${birthday.id}", 0)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return lastWhatsMessagedYear == currentYear
    }

    fun wasWhatsappMessageSent(context: Context, birthday: Birthday): Boolean {
        val prefs = context.getSharedPreferences("BirthdayPrefs", Context.MODE_PRIVATE)
        val lastWhatsMessagedYear = prefs.getInt("whatsapp_sent_${birthday.id}", 0)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return lastWhatsMessagedYear == currentYear
    }


//    fun scheduleSmsMessage(context: Context, birthday: Birthday) {
//        try {
//            if (wasSmsMessageSent(context, birthday)) {
//                Log.d("NotificationScheduler", "SMS already sent for ${birthday.name} this year. Skipping...")
//                return
//            }
//
//            val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
//            val birthdayDate = dateFormat.parse(birthday.date) ?: return
//
//            val birthdayCal = Calendar.getInstance()
//            val currentCal = Calendar.getInstance()
//
//            birthdayCal.time = birthdayDate
//            birthdayCal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR))
//            birthdayCal.set(Calendar.HOUR_OF_DAY, 0)
//            birthdayCal.set(Calendar.MINUTE, 0)
//            birthdayCal.set(Calendar.SECOND, 0)
//
//            if (birthdayCal.before(currentCal)) {
//                birthdayCal.add(Calendar.YEAR, 1)
//            }
//
//            val smsTime = birthdayCal.timeInMillis - TimeUnit.MINUTES.toMillis(149)
//
//            val smsIntent = Intent(context, WhatsappMessageReceiver::class.java)
//            smsIntent.putExtra("name", birthday.name)
//            smsIntent.putExtra("whatsapp", birthday.whatsapp)  // Phone number
//            smsIntent.putExtra("id", birthday.id)
//            smsIntent.putExtra("wish", birthday.wish)
//
//            val pendingIntent = PendingIntent.getBroadcast(
//                context,
//                WHATSAPP_REQUEST_CODE + birthday.id,
//                smsIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//
//            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                alarmManager.setExactAndAllowWhileIdle(
//                    AlarmManager.RTC_WAKEUP,
//                    smsTime,
//                    pendingIntent
//                )
//            } else {
//                alarmManager.setExact(
//                    AlarmManager.RTC_WAKEUP,
//                    smsTime,
//                    pendingIntent
//                )
//            }
//
//            Log.d("NotificationScheduler", "Scheduled SMS for ${birthday.name} at ${Date(smsTime)}")
//
//        } catch (e: Exception) {
//            Log.e("NotificationScheduler", "Error scheduling SMS: ${e.message}")
//        }
//    }

    fun scheduleSmsMessage(context: Context, birthday: Birthday) {
        try {
            if (wasSmsMessageSent(context, birthday)) {
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

            val smsTime = birthdayCal.timeInMillis - TimeUnit.MINUTES.toMillis(149)

            if (smsTime <= System.currentTimeMillis()) {
                birthdayCal.add(Calendar.YEAR, 1) // Move to next year
            }

            val adjustedSmsTime = birthdayCal.timeInMillis - TimeUnit.MINUTES.toMillis(149)

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
                    AlarmManager.RTC_WAKEUP, adjustedSmsTime, pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, adjustedSmsTime, pendingIntent)
            }

            Log.d("NotificationScheduler", "Scheduled SMS for ${birthday.name} at ${Date(adjustedSmsTime)}")

        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Error scheduling SMS: ${e.message}")
        }
    }




//    fun scheduleWhatsAppMessage(context: Context, birthday: Birthday) {
//        try {
//
//            if (wasWhatsappMessageSent(context, birthday) ) {
//                Log.d("NotificationScheduler", "Whatsapp message already sent for ${birthday.name} this year. Skipping...")
//                return
//            }
//
//            // Parse the birthday date (assuming format is DD/MM)
//            val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
//            val birthdayDate = dateFormat.parse(birthday.date) ?: return
//
//            // Get calendar instance for the next occurrence of this birthday
//            val birthdayCal = Calendar.getInstance()
//            val currentCal = Calendar.getInstance()
//
//            // Set birthday time to the date with hours set to 00:00:00
//            birthdayCal.time = birthdayDate
//            birthdayCal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR))
//            birthdayCal.set(Calendar.HOUR_OF_DAY, 0)
//            birthdayCal.set(Calendar.MINUTE, 0)
//            birthdayCal.set(Calendar.SECOND, 0)
//
//            // If the birthday has already passed this year, schedule for next year
//            if (birthdayCal.before(currentCal)) {
//                birthdayCal.add(Calendar.YEAR, 1)
//            }
//
//            // Calculate midnight time for the birthday
////            val whatsappMessageTime = birthdayCal.timeInMillis
//            val whatsappMessageTime = birthdayCal.timeInMillis - TimeUnit.MINUTES.toMillis(101)
//
//
//            // Create intent for WhatsApp message
//            val whatsappIntent = Intent(context, WhatsappMessageReceiver::class.java)
//            whatsappIntent.putExtra("name", birthday.name)
//            whatsappIntent.putExtra("whatsapp", birthday.whatsapp)
//            whatsappIntent.putExtra("id", birthday.id)
//            whatsappIntent.putExtra("wish", birthday.wish)
//
//            // Create pending intent
//            val pendingIntent = PendingIntent.getBroadcast(
//                context,
//                WHATSAPP_REQUEST_CODE + birthday.id, // Unique request code for each birthday
//                whatsappIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//
//            // Get alarm manager
//            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//            // Schedule the WhatsApp message alarm
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                alarmManager.setExactAndAllowWhileIdle(
//                    AlarmManager.RTC_WAKEUP,
//                    whatsappMessageTime,
//                    pendingIntent
//                )
//            } else {
//                alarmManager.setExact(
//                    AlarmManager.RTC_WAKEUP,
//                    whatsappMessageTime,
//                    pendingIntent
//                )
//            }
//
//            Log.d("NotificationScheduler", "Scheduled WhatsApp message for ${birthday.name} at ${Date(whatsappMessageTime)}")
//
//        } catch (e: Exception) {
//            Log.e("NotificationScheduler", "Error scheduling WhatsApp message: ${e.message}")
//        }
//    }

//    fun scheduleWhatsAppMessage(context: Context, birthday: Birthday) {
//        try {
//            if (wasWhatsappMessageSent(context, birthday)) {
//                Log.d("NotificationScheduler", "WhatsApp message already sent for ${birthday.name} this year. Skipping...")
//                return
//            }
//
//            val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
//            val birthdayDate = dateFormat.parse(birthday.date) ?: return
//
//            val birthdayCal = Calendar.getInstance()
//            val currentCal = Calendar.getInstance()
//
//            birthdayCal.time = birthdayDate
//            birthdayCal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR))
//            birthdayCal.set(Calendar.HOUR_OF_DAY, 0)
//            birthdayCal.set(Calendar.MINUTE, 0)
//            birthdayCal.set(Calendar.SECOND, 0)
//
//            val whatsappMessageTime = birthdayCal.timeInMillis + TimeUnit.MINUTES.toMillis(30)
//
//            if (whatsappMessageTime <= System.currentTimeMillis()) {
//                birthdayCal.add(Calendar.YEAR, 1) // Move to next year
//            }
//
//            val adjustedWhatsappMessageTime = birthdayCal.timeInMillis + TimeUnit.MINUTES.toMillis(30)
//
//            val whatsappIntent = Intent(context, WhatsappMessageReceiver::class.java).apply {
//                putExtra("name", birthday.name)
//                putExtra("whatsapp", birthday.whatsapp)
//                putExtra("id", birthday.id)
//                putExtra("wish", birthday.wish)
//            }
//
//            val pendingIntent = PendingIntent.getBroadcast(
//                context, WHATSAPP_REQUEST_CODE + birthday.id, whatsappIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//
//            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                alarmManager.setExactAndAllowWhileIdle(
//                    AlarmManager.RTC_WAKEUP, adjustedWhatsappMessageTime, pendingIntent
//                )
//            } else {
//                alarmManager.setExact(AlarmManager.RTC_WAKEUP, adjustedWhatsappMessageTime, pendingIntent)
//            }
//
//            Log.d("NotificationScheduler", "Scheduled WhatsApp message for ${birthday.name} at ${Date(adjustedWhatsappMessageTime)}")
//
//        } catch (e: Exception) {
//            Log.e("NotificationScheduler", "Error scheduling WhatsApp message: ${e.message}")
//        }
//    }

}