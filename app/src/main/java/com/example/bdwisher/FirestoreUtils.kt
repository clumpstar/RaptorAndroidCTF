package com.example.bdwisher

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreUtils {
    fun getFlagFromFirestore(context: Context, index: Int, callback: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val documentId = context.getString(R.string.document)

        val flagList = arrayListOf<String>()

        db.collection("flags").document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    for (i in 1..8) { // Assuming flag1 to flag5 exist
                        val flag = document.getString("flag$i") ?: "N/A"
                        flagList.add(flag)
                    }

                    // Return the flag at the specified index (adjusted for 0-based index)
                    callback(flagList.getOrElse(index - 1) { "N/A" }) // Adjusted index
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
