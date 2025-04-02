package com.example.bdwisher

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class UnusedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_unused)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getFlagFromFirestore(this) { flag ->
            showFlag(flag)
        }

    }

    private fun showFlag(flag: String) {
        AlertDialog.Builder(this)
            .setTitle("Captured Flag")
            .setMessage(flag)
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    private fun getFlagFromFirestore(context: Context, callback: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val documentId = context.getString(R.string.document)

        db.collection("flags").document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val flag = document.getString("flag1") ?: "N/A"
                    Log.d("HiddenActivity", "Fetched flag: $flag")
                    callback(flag)
                } else {
                    Log.e("HiddenActivity", "Document not found!")
                    callback("N/A")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HiddenActivity", "Error fetching flag: ${exception.message}")
                callback("N/A")
            }
    }


}