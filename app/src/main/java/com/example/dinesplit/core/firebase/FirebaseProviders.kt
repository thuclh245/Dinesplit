package com.example.dinesplit.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

object FirebaseProviders {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    // VNPAY callable Functions integration is temporarily disabled.
    // val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance("asia-southeast1") }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    val messaging: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }
}
