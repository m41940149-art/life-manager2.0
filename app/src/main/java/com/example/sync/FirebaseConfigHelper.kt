package com.example.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseConfigHelper {

    fun isFirebaseConfigured(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                true
            } else {
                FirebaseApp.initializeApp(context) != null
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        return if (isFirebaseConfigured(context)) {
            try {
                FirebaseFirestore.getInstance()
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }
}
