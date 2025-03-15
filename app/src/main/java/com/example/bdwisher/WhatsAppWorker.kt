package com.example.bdwisher

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.bdwisher.NotificationScheduler.WHATSAPP_REQUEST_CODE
import com.example.bdwisher.NotificationScheduler.wasWhatsappMessageSent
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WhatsAppWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val name = inputData.getString("name") ?: return Result.failure()
        val whatsapp = inputData.getString("whatsapp") ?: return Result.failure()
        val id = inputData.getInt("id", 0)
        val wish = inputData.getString("wish") ?: return Result.failure()
        val date = inputData.getString("date") ?: return Result.failure()

        val birthday = Birthday(id, name, date, whatsapp, wish)

        scheduleWhatsAppMessage(applicationContext, birthday)

        return Result.success()
    }

    private fun scheduleWhatsAppMessage(context: Context, birthday: Birthday) {
        try {
            if (wasWhatsappMessageSent(context, birthday)) {
                Log.d("WhatsAppWorker", "WhatsApp message already sent for ${birthday.name}. Skipping...")
                return
            }

            val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
            Log.e("WhatsAppWorker", "The date passed is: ${birthday.date}")
            val birthdayDate = dateFormat.parse(birthday.date) ?: return
            Log.e("WhatsAppWorker", "The date passed is: ${birthdayDate}")
            val birthdayCal = Calendar.getInstance()
            val currentCal = Calendar.getInstance()

            birthdayCal.time = birthdayDate
            birthdayCal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR))
            birthdayCal.set(Calendar.HOUR_OF_DAY, 0)
            birthdayCal.set(Calendar.MINUTE, 0)
            birthdayCal.set(Calendar.SECOND, 0)

            val whatsappMessageTime = birthdayCal.timeInMillis + TimeUnit.MINUTES.toMillis(748)

            if (whatsappMessageTime <= System.currentTimeMillis()) {
                birthdayCal.add(Calendar.YEAR, 1) // Move to next year
            }

            val adjustedWhatsappMessageTime = birthdayCal.timeInMillis + TimeUnit.MINUTES.toMillis(748)

            val whatsappIntent = Intent(context, WhatsappMessageReceiver::class.java).apply {
                putExtra("name", birthday.name)
                putExtra("whatsapp", birthday.whatsapp)
                putExtra("id", birthday.id)
                putExtra("wish", birthday.wish)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, WHATSAPP_REQUEST_CODE + birthday.id, whatsappIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, adjustedWhatsappMessageTime, pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, adjustedWhatsappMessageTime, pendingIntent)
            }

            Log.d("WhatsAppWorker", "Scheduled WhatsApp message for ${birthday.name} at ${Date(adjustedWhatsappMessageTime)}")

        } catch (e: Exception) {
            Log.e("WhatsAppWorker", "Error scheduling WhatsApp message: ${e.message}")
        }
    }
}
