package com.example.bdwisher

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class SurpriseProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.bdwisher.provider"
        const val PATH_FLAG = "surprise"

        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_FLAG")

        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.example.bdwisher.surprise"
        const val COLUMN_FLAG = "flag"

        private const val SECRET_KEY = "TODAY_IS_MY_BIRTHDAY"

        // URI matcher codes
        private const val FLAG_CODE = 1

        // UriMatcher setup
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_FLAG, FLAG_CODE)
        }
    }

    private var fetchedFlag: String = "Default_Flag"

    override fun onCreate(): Boolean {
        // Fetch a default flag from Firestore for initialization
        context?.let { ctx ->
            FirestoreUtils.getFlagFromFirestore(ctx, 5) { flag ->
                fetchedFlag = flag // Store the fetched flag for later use
            }
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        if (uriMatcher.match(uri) != FLAG_CODE) {
            throw IllegalArgumentException("Unknown URI: $uri")
        }

        val cursor = MatrixCursor(arrayOf(COLUMN_FLAG))

        if (selection == SECRET_KEY) {
            cursor.addRow(arrayOf(fetchedFlag))
        } else {
            cursor.addRow(arrayOf("Access denied. Incorrect key provided."))
        }

        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            FLAG_CODE -> CONTENT_TYPE
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Insert operation is not supported")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("Delete operation is not supported")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        throw UnsupportedOperationException("Update operation is not supported")
    }
}
