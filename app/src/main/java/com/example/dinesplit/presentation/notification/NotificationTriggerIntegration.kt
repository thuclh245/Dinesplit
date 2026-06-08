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
 * Điểm tích hợp dùng để các module khác phát sinh thông báo mà không cần biết chi tiết lưu trữ.
 *
 * Các module Feed, Split và Personal chỉ cần tạo trigger domain rồi gọi object này. Integration
 * sẽ chuyển trigger thành [Notification] thông qua [NotificationFactory] và lưu vào repository
 * trên coroutine IO riêng để không chặn luồng UI.
 */
@Suppress("unused")
object NotificationTriggerIntegration {
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Tạo thông báo từ sự kiện Feed như thích, bình luận hoặc tương tác bài viết.
     *
     * @param context Context dùng để lấy repository từ [AppContainer].
     * @param trigger Dữ liệu sự kiện feed cần chuyển thành thông báo.
     * @param userId UID người nhận thông báo.
     */
    fun triggerFeedNotification(
        context: Context,
        trigger: FeedNotificationTrigger,
        userId: String,
    ) {
        dispatchNotification(context) {
            NotificationFactory.fromFeedTrigger(trigger, userId)
        }
    }

    /**
     * Tạo thông báo từ sự kiện Split như tạo bill, nhắc thanh toán hoặc hoàn tất thanh toán.
     *
     * @param context Context dùng để lấy repository từ [AppContainer].
     * @param trigger Dữ liệu sự kiện split cần chuyển thành thông báo.
     * @param userId UID người nhận thông báo.
     */
    fun triggerSplitNotification(
        context: Context,
        trigger: SplitNotificationTrigger,
        userId: String,
    ) {
        dispatchNotification(context) {
            NotificationFactory.fromSplitTrigger(trigger, userId)
        }
    }

    /**
     * Tạo thông báo từ sự kiện tài chính cá nhân như reminder vượt ngưỡng ngân sách.
     *
     * @param context Context dùng để lấy repository từ [AppContainer].
     * @param trigger Dữ liệu sự kiện cá nhân cần chuyển thành thông báo.
     * @param userId UID người nhận thông báo.
     */
    fun triggerPersonalNotification(
        context: Context,
        trigger: PersonalNotificationTrigger,
        userId: String,
    ) {
        dispatchNotification(context) {
            NotificationFactory.fromPersonalTrigger(trigger, userId)
        }
    }

    /**
     * Thực thi việc ghi thông báo trên coroutine IO riêng.
     *
     * @param context Context nguồn gọi; hàm luôn chuyển sang application context để tránh giữ Activity.
     * @param buildNotification Lambda tạo [Notification] tại thời điểm dispatch.
     */
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
