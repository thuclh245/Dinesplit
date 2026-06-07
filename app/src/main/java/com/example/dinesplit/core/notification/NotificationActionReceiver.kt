package com.example.dinesplit.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra("notification_id", -1)

        // Dismiss the notification
        if (notificationId != -1) {
            try {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(notificationId)
            } catch (e: Exception) {
                android.util.Log.e("NotificationActionReceiver", "Failed to cancel notification", e)
            }
        }

        // Initialize FcmManager if it has not been initialized
        FcmManager.initialize(context)

        when (action) {
            "com.example.dinesplit.ACTION_SNOOZE_1H" -> {
                val muteUntil = System.currentTimeMillis() + 3600_000L
                FcmManager.setMuteUntil(muteUntil)
                Toast.makeText(context, "Đã tắt thông báo trong 1 tiếng", Toast.LENGTH_SHORT).show()
            }
            "com.example.dinesplit.ACTION_MUTE_ALL" -> {
                FcmManager.setMutePermanently(true)
                Toast.makeText(context, "Đã tắt thông báo vĩnh viễn", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
