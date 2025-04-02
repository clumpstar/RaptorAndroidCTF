package com.example.bdwisher

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class SecretStorage {
    companion object {
        private var cachedFlag: String? = null  // Store flag for quick access

        fun initializeFlag(context: Context) {
            val db = FirebaseFirestore.getInstance()
            val documentId = context.getString(R.string.document)

            db.collection("flags").document(documentId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val flag = document.getString("flag3") ?: "N/A"
                        cachedFlag = flag
                        Log.d("Firestore", "Flag cached successfully: $flag")
                    } else {
                        Log.e("Firestore", "Document does not exist!")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error fetching flag: ${exception.message}")
                }
        }

        fun revealFlag(): String {
            return cachedFlag ?: "FLAG_NOT_READY"
        }

    }


}
