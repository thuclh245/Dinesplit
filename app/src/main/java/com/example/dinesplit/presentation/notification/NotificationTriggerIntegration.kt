package com.example.dinesplit.presentation.notification

import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.domain.model.FeedNotificationTrigger
import com.example.dinesplit.domain.model.NotificationFactory
import com.example.dinesplit.domain.model.PersonalNotificationTrigger
import com.example.dinesplit.domain.model.SplitNotificationTrigger
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Helper object to dispatch notification triggers from Feed / Split modules.
 * Call these when events happen in feed (like/comment) or split (payment/bill).
 *
 * Usage from Feed module when user likes a post:
 *   NotificationTriggerIntegration.triggerFeedNotification(
 *       context = this,  // from Activity or Fragment
 *       trigger = FeedNotificationTrigger(...),
 *       userId = recipientUserId
 *   )
 */
@Suppress("unused", "ObjectName")
object NotificationTriggerIntegration {
    @OptIn(DelicateCoroutinesApi::class)
    @Suppress("kotlin:S6808") // Suppress GlobalScope warning for notification dispatch
    fun triggerFeedNotification(
        context: android.content.Context,
        trigger: FeedNotificationTrigger,
        userId: String,
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val notificationRepo = AppContainer.notificationRepository(context)
                val notification = NotificationFactory.fromFeedTrigger(trigger, userId)
                notificationRepo.insertNotification(notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Suppress("kotlin:S6808")
    fun triggerSplitNotification(
        context: android.content.Context,
        trigger: SplitNotificationTrigger,
        userId: String,
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val notificationRepo = AppContainer.notificationRepository(context)
                val notification = NotificationFactory.fromSplitTrigger(trigger, userId)
                notificationRepo.insertNotification(notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Suppress("kotlin:S6808")
    fun triggerPersonalNotification(
        context: android.content.Context,
        trigger: PersonalNotificationTrigger,
        userId: String,
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val notificationRepo = AppContainer.notificationRepository(context)
                val notification = NotificationFactory.fromPersonalTrigger(trigger, userId)
                notificationRepo.insertNotification(notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
