package com.example.bdwisher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import java.util.Calendar

class SmsMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val name = intent.getStringExtra("name") ?: return
            val phoneNumber = intent.getStringExtra("whatsapp") ?: return
            val id = intent.getIntExtra("id", 0)

            val prefs = context.getSharedPreferences("BirthdayPrefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            editor.putInt("sms_sent_$id", currentYear)
            editor.apply()

            // Format the SMS message
            val message = intent.getStringExtra("wish") ?: return

            // Send SMS instead of WhatsApp
            sendSms(context, phoneNumber, message)

            Log.d("SmsMessageReceiver", "SMS sent successfully to $name")

        } catch (e: Exception) {
            Log.e("SmsMessageReceiver", "Error sending SMS: ${e.message}")
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)

            // Show a toast for confirmation
            Toast.makeText(context, "Birthday SMS sent!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("SmsMessageReceiver", "Failed to send SMS: ${e.message}")
        }
    }
}






















