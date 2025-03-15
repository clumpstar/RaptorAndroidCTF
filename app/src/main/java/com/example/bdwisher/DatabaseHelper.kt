package com.example.bdwisher

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.security.MessageDigest
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    val document = context.getString(R.string.document)
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE birthdays (id INTEGER PRIMARY KEY, name TEXT, date TEXT, whatsapp TEXT, wish TEXT, Hash TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS birthdays")
        onCreate(db)
    }

    fun addBirthday(name: String, date: String, whatsapp: String, wish:String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("date", date)
            put("whatsapp", whatsapp)
            put("wish", wish)
            put("Hash", hashString(getFlagFromFirestore(), "SHA-256"))
        }
        db.insert("birthdays", null, values)
//        db.close()
    }

    fun hashString(input: String, algorithm: String): String {
        val bytes = MessageDigest.getInstance(algorithm).digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) } // Convert bytes to hex
    }

    fun getFlagFromFirestore(): String{
        val db = FirebaseFirestore.getInstance()
        val documentId = document
        var flag = ""

        db.collection("flags").document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    flag = document.getString("flag1") ?: "N/A"
                } else {
                    println("Document does not exist!")
                }
            }
            .addOnFailureListener { exception ->
                println("Error fetching flags: ${exception.message}")
            }

        return flag

    }

    fun getAllBirthdays(): List<Birthday> {
        val list = mutableListOf<Birthday>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM birthdays", null)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val name = cursor.getString(1)
            val date = cursor.getString(2)
            val whatsapp = cursor.getString(3)
            val wish = cursor.getString(4)
            list.add(Birthday(id, name, date, whatsapp, wish))
        }
        cursor.close()
//        db.close()
        return list
    }

    fun deleteBirthday(context: Context, id: Int): Boolean {
        val db = writableDatabase
        val prefs = context.getSharedPreferences("BirthdayPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove("whatsapp_sent_$id")
        editor.remove("sms_sent_$id")
        editor.remove("notified_$id")
        editor.apply()
        val result = db.delete("birthdays", "id=?", arrayOf(id.toString()))
//        db.close()
        return result > 0 // Return true if a row was deleted, false otherwise
    }

    fun getBirthdayById(id: Int): Birthday? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM birthdays WHERE id=?", arrayOf(id.toString()))

        return if (cursor.moveToFirst()) {
            val name = cursor.getString(1)
            val date = cursor.getString(2)
            val whatsapp = cursor.getString(3)
            val wish = cursor.getString(4)
            cursor.close()
            Birthday(id, name, date, whatsapp, wish)
        } else {
            cursor.close()
            null
        }
    }

    companion object {
        private const val DATABASE_NAME = "birthdays.db"
        private const val DATABASE_VERSION = 3
    }
}
