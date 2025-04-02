package com.example.bdwisher

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class BirthdayNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "birthday_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val name = intent.getStringExtra("name") ?: return
            val id = intent.getIntExtra("id", 0)
            val date = intent.getStringExtra("date") ?: return


            // Store that notification has been shown
            val prefs = context.getSharedPreferences("BirthdayPrefs", Context.MODE_PRIVATE)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            prefs.edit().putInt("notified_$id", currentYear).apply()

            // Create an intent to open the app when notification is tapped
            val contentIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            getFlagFromFirestore(context) { firstFlag ->
                // Create notification
                val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Birthday Reminder")
                    .setContentText("$name's birthday is tomorrow ($date)!")
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("Don't forget to wish $name his birthday wishes with $firstFlag"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                // Show notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(id, builder.build())
            }

            Log.d("BirthdayNotification", "Notification sent for $name's birthday")

        } catch (e: Exception) {
            Log.e("BirthdayNotification", "Error showing notification: ${e.message}")
        }
    }

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
                    callback(flagList.getOrElse(2) { "N/A" })
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
}