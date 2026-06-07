package com.example.dinesplit.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dinesplit.MainActivity
import com.example.dinesplit.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

object NotificationHelper {
    const val CHANNEL_ID = "dinesplit_general"
    private const val NOTIFICATION_ID_BASE = 1000
    private var lastNotificationTime = 0L

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "DineSplit"
            val descriptionText = "Thông báo về bài viết, nhóm và hóa đơn"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String>? = null,
        avatarUrl: String? = null
    ) {
        // Check settings and snooze state
        if (!FcmManager.isNotificationsEnabled()) {
            return
        }
        if (FcmManager.isMutePermanently()) {
            return
        }
        if (System.currentTimeMillis() < FcmManager.getMuteUntil()) {
            return
        }

        // Spam rule: Cooldown of 10 seconds between notifications
        val now = System.currentTimeMillis()
        if (now - lastNotificationTime < 10_000L) {
            return
        }
        lastNotificationTime = now

        val id = (System.currentTimeMillis() % 100000).toInt() + NOTIFICATION_ID_BASE

        // Build click intent to open MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Put custom data payload as extras for deep-linking
            data?.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.dinesplit.ACTION_SNOOZE_1H"
            putExtra("notification_id", id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            id * 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.dinesplit.ACTION_MUTE_ALL"
            putExtra("notification_id", id)
        }
        val mutePendingIntent = PendingIntent.getBroadcast(
            context,
            id * 2 + 1,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun postNotification(largeIcon: android.graphics.Bitmap?) {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)  // White-on-transparent icon for status bar
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(0xFFC9401A.toInt()) // DineSplit brand terracotta color
                .addAction(R.drawable.ic_notification, "Tắt trong 1 tiếng", snoozePendingIntent)
                .addAction(R.drawable.ic_notification, "Tắt luôn", mutePendingIntent)

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }

            try {
                val notificationManager = NotificationManagerCompat.from(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                }
                notificationManager.notify(id, builder.build())
            } catch (e: Exception) {
                android.util.Log.e("NotificationHelper", "Failed to show notification", e)
            }
        }

        if (!avatarUrl.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                val largeIcon = try {
                    val request = ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .allowHardware(false) // Required for notification bitmaps
                        .build()
                    val result = context.imageLoader.execute(request)
                    if (result is SuccessResult) {
                        val drawable = result.drawable
                        val size = 192
                        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        drawable.setBounds(0, 0, size, size)
                        drawable.draw(canvas)
                        bitmap
                    } else null
                } catch (e: Exception) {
                    android.util.Log.w("NotificationHelper", "Cannot render avatar as large icon", e)
                    null
                }
                postNotification(largeIcon)
            }
        } else {
            postNotification(null)
        }
    }
}
