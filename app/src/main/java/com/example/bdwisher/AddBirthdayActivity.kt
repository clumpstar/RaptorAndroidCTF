package com.example.bdwisher

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AddBirthdayActivity : AppCompatActivity() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var dateEditText: TextInputEditText
    private lateinit var whatsappEditText: TextInputEditText
    private lateinit var wishEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var backButton: MaterialButton
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_birthday)

        // Initialize UI components
        nameEditText = findViewById(R.id.nameEditText)
        dateEditText = findViewById(R.id.dateEditText)
        whatsappEditText = findViewById(R.id.whatsappEditText)
        wishEditText = findViewById(R.id.wishEditText)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)

        // Initialize database helper
        dbHelper = DatabaseHelper(this)

        // Set up date picker dialog for the date field
        dateEditText.setOnClickListener {
            showDatePickerDialog()
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
                // Format the selected date
                val selectedDate = Calendar.getInstance()
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Format as DD/MM
                val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)

                // Set the formatted date in the EditText
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

        dbHelper.addBirthday(name, date, whatsapp, wish)

        val birthdays = dbHelper.getAllBirthdays()

        // Check if exactly 7 birthdays exist and fetch flag
        if (birthdays.size == 7) {
            (applicationContext as? MainActivity)?.getFlagFromFirestore(this,2) { flag ->
                (applicationContext as? MainActivity)?.storeFlagInSharedPreferences(flag)
            }
        }

        Toast.makeText(this, "Birthday saved successfully", Toast.LENGTH_SHORT).show()
        finish()
    }

}