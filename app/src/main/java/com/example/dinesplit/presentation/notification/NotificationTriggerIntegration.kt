package com.example.dinesplit.presentation.notification

import android.content.Context
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.domain.model.FeedNotificationTrigger
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationFactory
import com.example.dinesplit.domain.model.PersonalNotificationTrigger
import com.example.dinesplit.domain.model.SplitNotificationTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Đối tượng trợ giúp để gửi các kích hoạt thông báo từ các mô-đun Feed / Split.
 * Gọi những cái này khi sự kiện xảy ra trong feed (like/comment) hoặc split (payment/bill).
 *
 * Cách sử dụng từ mô-đun Feed khi người dùng thích một bài đăng:
 *   NotificationTriggerIntegration.triggerFeedNotification(
 *       context = this,  // từ Activity hoặc Fragment
 *       trigger = FeedNotificationTrigger(...),
 *       userId = recipientUserId
 *   )
 */
@Suppress("unused")
object NotificationTriggerIntegration {
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun triggerFeedNotification(
        context: Context,
        trigger: FeedNotificationTrigger,
        userId: String,
    ) {
        dispatchNotification(context) {
            NotificationFactory.fromFeedTrigger(trigger, userId)
        }
    }

    fun triggerSplitNotification(
        context: Context,
        trigger: SplitNotificationTrigger,
        userId: String,
    ) {
        dispatchNotification(context) {
            NotificationFactory.fromSplitTrigger(trigger, userId)
        }
    }

    fun triggerPersonalNotification(
        context: Context,
        trigger: PersonalNotificationTrigger,
        userId: String,
    ) {
        dispatchNotification(context) {
            NotificationFactory.fromPersonalTrigger(trigger, userId)
        }
    }

    private fun dispatchNotification(
        context: Context,
        buildNotification: () -> Notification,
    ) {
        val appContext = context.applicationContext
        notificationScope.launch {
            runCatching {
                val notificationRepo = AppContainer.notificationRepository(appContext)
                notificationRepo.insertNotification(buildNotification())
            }.onFailure { throwable ->
                throwable.printStackTrace()
            }
        }
    }
}
