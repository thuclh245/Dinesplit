package com.example.dinesplit.data.repository

import android.util.Log
import com.example.dinesplit.core.notification.FcmManager
import com.example.dinesplit.core.notification.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DineSplitMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Do not log the sensitive token in production logs
        Log.d("DineSplitMessaging", "onNewToken triggered")
        FcmManager.handleNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("DineSplitMessaging", "onMessageReceived from: ${remoteMessage.from}")

        val notificationTitle = remoteMessage.notification?.title
        val notificationBody = remoteMessage.notification?.body
        val data = remoteMessage.data

        val title = notificationTitle ?: data["title"]
        val body = notificationBody ?: data["body"]
        val avatarUrl = data["avatarUrl"] ?: data["senderAvatarUrl"] ?: data["avatar"]

        if (!title.isNullOrBlank() && !body.isNullOrBlank()) {
            NotificationHelper.showNotification(
                context = applicationContext,
                title = title,
                body = body,
                data = data,
                avatarUrl = avatarUrl
            )
        }
    }
}
