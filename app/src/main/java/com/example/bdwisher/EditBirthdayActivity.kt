package com.example.bdwisher

import android.app.Activity
import android.app.Application
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class EditBirthdayActivity : AppCompatActivity() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var dateEditText: TextInputEditText
    private lateinit var whatsappEditText: TextInputEditText
    private lateinit var wishEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var delButton: MaterialButton
    private lateinit var backButton: MaterialButton
    private lateinit var dbHelper: DatabaseHelper

    private var birthdayId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_birthday)

        // Initialize UI components
        nameEditText = findViewById(R.id.nameEditText)
        dateEditText = findViewById(R.id.dateEditText)
        whatsappEditText = findViewById(R.id.whatsappEditText)
        wishEditText = findViewById(R.id.wishEditText)
        saveButton = findViewById(R.id.saveButton)
        delButton = findViewById(R.id.delButton)
        backButton = findViewById(R.id.backButton)

        // Initialize database helper
        dbHelper = DatabaseHelper(this)

        birthdayId = intent.getIntExtra("BIRTHDAY_ID", -1)

        if (birthdayId != -1) {
            val birthday = dbHelper.getBirthdayById(birthdayId)
            if (birthday != null) {
                nameEditText.setText(birthday.name)
                dateEditText.setText(birthday.date)
                whatsappEditText.setText(birthday.whatsapp)
                wishEditText.setText(birthday.wish)
            }
        }

        // Set up date picker dialog for the date field
        dateEditText.setOnClickListener {
            showDatePickerDialog()
        }

        // Set up delete button click listener
        delButton.setOnClickListener {
            deleteBirthday()
        }

        // Set up save button click listener
        saveButton.setOnClickListener {
            saveBirthday()
        }

        backButton.setOnClickListener{
            finish()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)

                dateEditText.setText(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun saveBirthday() {
        val name = nameEditText.text.toString().trim()
        val date = dateEditText.text.toString().trim()
        val whatsapp = whatsappEditText.text.toString().trim()
        val wish = wishEditText.text.toString().trim()

        if (name.isEmpty() || date.isEmpty() || whatsapp.isEmpty() || wish.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (birthdayId != -1) {
            dbHelper.deleteBirthday(this, birthdayId) // Delete old entry
        }
        dbHelper.addBirthday(name, date, whatsapp, wish) // Insert new entry

        val birthdays = dbHelper.getAllBirthdays()
        val newBirthday = birthdays.lastOrNull()

        if (newBirthday != null) {
            val scheduler = NotificationScheduler(this)
            scheduler.scheduleBirthdayNotification(newBirthday)
        }

        Toast.makeText(this, "Birthday saved successfully", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deleteBirthday() {
        if (birthdayId != -1) {
            val success = dbHelper.deleteBirthday(this, birthdayId)
            if (success) {
                Toast.makeText(this, "Birthday deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to delete birthday", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
