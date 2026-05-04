package com.example.dinesplit.core.firebase
import android.util.Log
object FirebaseBaselineCheck {
    private const val TAG = "FirebaseBaseline"
    fun verify() {
        val currentUser = FirebaseProviders.auth.currentUser
        Log.d(TAG, "FirebaseAuth currentUser=${currentUser?.uid ?: "null"}")
        Log.d(TAG, "FirebaseFirestore instance=${FirebaseProviders.firestore}")
        Log.d(TAG, "FirebaseStorage instance=${FirebaseProviders.storage}")
        Log.d(TAG, "FirebaseMessaging instance=${FirebaseProviders.messaging}")
    }
}
