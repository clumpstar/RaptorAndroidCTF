
package com.example.bdwisher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.util.Calendar

class WhatsappMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val name = intent.getStringExtra("name") ?: return
            val whatsappNumber = intent.getStringExtra("whatsapp") ?: return
            val id = intent.getIntExtra("id", 0)

            val prefs = context.getSharedPreferences("BirthdayPrefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            editor.putInt("whatsapp_sent_$id", currentYear)
            editor.apply()

            // Format the WhatsApp message
            val message = intent.getStringExtra("wish") ?: return
//            val message = "Happy Birthday $name! 🎂🎉 Wishing you a fantastic day filled with joy and happiness!"

            // Create WhatsApp intent
            sendWhatsAppMessage(context, whatsappNumber, message)

            Log.d("WhatsAppMessage", "WhatsApp message initiated for $name")

        } catch (e: Exception) {
            Log.e("WhatsAppMessage", "Error sending WhatsApp message: ${e.message}")
        }
    }

    private fun sendWhatsAppMessage(context: Context, phoneNumber: String, message: String) {
        try {
            // Format phone number (remove spaces and add country code if not present)
            var formattedNumber = phoneNumber.replace(Regex("[^\\d+]"), "")
            Log.d("WhatsappMessage", "The number is $formattedNumber")
            if (!formattedNumber.startsWith("+")) {
                formattedNumber = "+91$formattedNumber"  // Assuming India, modify as needed
            }

            // Create WhatsApp URI
            val messageEncoded = Uri.encode(message)
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=$messageEncoded")

            // Create intent to open WhatsApp
            val whatsappIntent = Intent(Intent.ACTION_VIEW, uri)

            // Set flags to open in a new task
            whatsappIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            // Check if WhatsApp is installed
            if (isWhatsAppInstalled(context)) {
                context.startActivity(whatsappIntent)
            } else {
                // Show toast if WhatsApp is not installed
                Toast.makeText(context, "WhatsApp is not installed on your device", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("WhatsAppMessage", "Error opening WhatsApp: ${e.message}")
        }
    }

    private fun isWhatsAppInstalled(context: Context): Boolean {
        val packageManager = context.packageManager
        return try {
            packageManager.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}