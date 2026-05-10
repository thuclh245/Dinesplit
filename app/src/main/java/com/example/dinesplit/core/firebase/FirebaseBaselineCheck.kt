package com.example.dinesplit.core.firebase

import android.util.Log

object FirebaseBaselineCheck {
    private const val TAG = "FirebaseBaseline"

    fun verify() {
        runCatching {
            val currentUser = FirebaseProviders.auth.currentUser
            Log.d(TAG, "FirebaseAuth currentUser=${currentUser?.uid ?: "null"}")
            Log.d(TAG, "FirebaseFirestore instance=${FirebaseProviders.firestore}")
            Log.d(TAG, "FirebaseStorage instance=${FirebaseProviders.storage}")
            Log.d(TAG, "FirebaseMessaging instance=${FirebaseProviders.messaging}")
        }.onFailure { throwable ->
            Log.e(TAG, "Firebase baseline check failed safely; startup will continue", throwable)
        }
    }
}
