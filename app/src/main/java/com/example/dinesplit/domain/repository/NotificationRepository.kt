package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.Notification
import kotlinx.coroutines.flow.Flow

/**
 * Interface định nghĩa các thao tác với hệ thống thông báo của ứng dụng.
 * Hỗ trợ lắng nghe thời gian thực, truy vấn danh sách, tạo mới và đánh dấu trạng thái đọc của thông báo.
 */
interface NotificationRepository {
    /**
     * Lắng nghe danh sách thông báo theo thời gian thực qua [Flow].
     * Mỗi khi có thông báo mới hoặc trạng thái thông báo thay đổi, Flow sẽ phát ra danh sách mới.
     *
     * @return [Flow] phát ra danh sách [Notification] cập nhật liên tục.
     */
    fun observeNotifications(): Flow<List<Notification>>

    /**
     * Truy vấn danh sách thông báo một lần duy nhất từ cơ sở dữ liệu.
     *
     * @return Danh sách [Notification] hiện có.
     */
    suspend fun getNotifications(): List<Notification>

    /**
     * Chèn một thông báo mới vào cơ sở dữ liệu.
     *
     * @param notification Đối tượng thông báo [Notification] cần lưu.
     */
    suspend fun insertNotification(notification: Notification)

    /**
     * Đánh dấu một thông báo là đã đọc.
     *
     * @param notificationId ID của thông báo cần đánh dấu.
     */
    suspend fun markAsRead(notificationId: String)

    /**
     * Đánh dấu một thông báo là chưa đọc.
     *
     * @param notificationId ID của thông báo cần đánh dấu.
     */
    suspend fun markAsUnread(notificationId: String)
}
