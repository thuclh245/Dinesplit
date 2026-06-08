package com.example.dinesplit.data.repository

import android.content.Context
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationDestination
import com.example.dinesplit.domain.model.NotificationType
import com.example.dinesplit.domain.repository.NotificationRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository lưu trữ và đọc thông báo người dùng bằng Firebase Firestore.
 *
 * Dữ liệu được tách theo collection `user_notifications/{uid}/notifications` để mỗi người dùng
 * chỉ quan sát và thao tác với thông báo của chính họ.
 *
 * @property firestore Thực thể Firestore dùng để đọc/ghi thông báo.
 */
class FirebaseNotificationRepository private constructor(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val firestore: FirebaseFirestore = FirebaseProviders.firestore,
) : NotificationRepository {
    /**
     * Lắng nghe danh sách thông báo của người dùng hiện tại theo thời gian thực.
     *
     * @return [Flow] phát ra danh sách [Notification] đã lọc dữ liệu demo cũ và sắp xếp mới nhất trước.
     */
    override fun observeNotifications(): Flow<List<Notification>> =
        callbackFlow {
            val uid = FirebaseProviders.auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val registration =
                firestore
                    .collection(COLLECTION_USER_NOTIFICATIONS)
                    .document(uid)
                    .collection(COLLECTION_NOTIFICATIONS)
                    .orderBy(FIELD_CREATED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.w("FirebaseNotifRepo", "observeNotifications: ${error.message}")
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        trySend(
                            snapshot?.documents
                                ?.mapNotNull { document -> document.toNotification(uid) }
                                .orEmpty()
                                .withoutLegacyDemoNotifications()
                                .orderedNewestFirst()
                        )
                    }

            awaitClose { registration.remove() }
        }

    /**
     * Tải danh sách thông báo một lần từ server Firestore.
     *
     * @return Danh sách thông báo hiện có của người dùng hiện tại.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun getNotifications(): List<Notification> {
        val uid = requireCurrentUserId()
        val snapshot =
            firestore
                .collection(COLLECTION_USER_NOTIFICATIONS)
                .document(uid)
                .collection(COLLECTION_NOTIFICATIONS)
                .orderBy(FIELD_CREATED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get(Source.SERVER)
                .awaitFirebase()

        return snapshot.documents.mapNotNull { document ->
            document.toNotification(uid)
        }.withoutLegacyDemoNotifications().orderedNewestFirst()
    }

    /**
     * Lưu một thông báo mới vào nhánh thông báo của người nhận.
     *
     * @param notification Thông báo cần lưu. Nếu [Notification.userId] rỗng, hàm dùng UID đăng nhập hiện tại.
     * @throws IllegalStateException nếu không xác định được người dùng nhận thông báo.
     */
    override suspend fun insertNotification(notification: Notification) {
        val uid = notification.userId.ifBlank { requireCurrentUserId() }
        firestore
            .collection(COLLECTION_USER_NOTIFICATIONS)
            .document(uid)
            .collection(COLLECTION_NOTIFICATIONS)
            .document(notification.id)
            .set(notification.toFirestoreMap())
            .awaitFirebase()
    }

    /**
     * Đánh dấu một thông báo là đã đọc.
     *
     * @param notificationId ID thông báo cần cập nhật.
     */
    override suspend fun markAsRead(notificationId: String) {
        updateReadState(notificationId = notificationId, isRead = true)
    }

    /**
     * Đánh dấu một thông báo là chưa đọc.
     *
     * @param notificationId ID thông báo cần cập nhật.
     */
    override suspend fun markAsUnread(notificationId: String) {
        updateReadState(notificationId = notificationId, isRead = false)
    }

    /**
     * Cập nhật trạng thái đọc/chưa đọc và thời điểm cập nhật cuối cùng cho một thông báo.
     *
     * @param notificationId ID thông báo cần cập nhật.
     * @param isRead Trạng thái đọc mới.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    private suspend fun updateReadState(
        notificationId: String,
        isRead: Boolean,
    ) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_NOTIFICATIONS)
            .document(uid)
            .collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .update(
                mapOf(
                    FIELD_IS_READ to isRead,
                    FIELD_UPDATED_AT to System.currentTimeMillis(),
                ),
            )
            .awaitFirebase()
    }

    /**
     * Lấy UID của người dùng đang đăng nhập.
     *
     * @return UID hiện tại.
     * @throws IllegalStateException nếu Firebase Auth chưa có người dùng.
     */
    private fun requireCurrentUserId(): String {
        return FirebaseProviders.auth.currentUser?.uid
            ?: throw IllegalStateException("Vui lòng đăng nhập để sử dụng thông báo")
    }

    /**
     * Đọc trường thời gian từ Firestore theo cách chịu lỗi.
     *
     * Firestore có thể trả về Timestamp hoặc Long tùy nguồn ghi dữ liệu, nên hàm thử cả hai kiểu.
     *
     * @param field Tên trường cần đọc.
     * @return Thời điểm dạng epoch millis hoặc null nếu dữ liệu thiếu/không hợp lệ.
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.getLongDateSafe(field: String): Long? {
        return try {
            getTimestamp(field)?.toDate()?.time
        } catch (e: Exception) {
            try {
                getLong(field)
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Chuyển một document Firestore thành domain model [Notification].
     *
     * @param uid UID fallback khi document không có trường `userId`.
     * @return [Notification] hợp lệ hoặc null nếu thiếu trường bắt buộc.
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.toNotification(uid: String): Notification? {
        val type =
            getString(FIELD_TYPE)?.let { value ->
                NotificationType.entries.firstOrNull { it.name == value }
            } ?: return null

        return Notification(
            id = getString(FIELD_ID) ?: id,
            userId = getString(FIELD_USER_ID) ?: uid,
            title = getString(FIELD_TITLE) ?: return null,
            subtitle = getString(FIELD_SUBTITLE) ?: return null,
            type = type,
            relatedId = getString(FIELD_RELATED_ID)?.takeIf { it.isNotBlank() },
            isRead = getBoolean(FIELD_IS_READ) ?: false,
            createdAt = getLongDateSafe(FIELD_CREATED_AT) ?: return null,
            updatedAt = getLongDateSafe(FIELD_UPDATED_AT) ?: System.currentTimeMillis(),
            deepLinkDestination = getString(FIELD_DEEP_LINK_DESTINATION)?.takeIf { it.isNotBlank() },
            deepLinkTargetId = getString(FIELD_DEEP_LINK_TARGET_ID)?.takeIf { it.isNotBlank() },
            senderId = getString(FIELD_SENDER_ID)?.takeIf { it.isNotBlank() },
            groupId = getString(FIELD_GROUP_ID)?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Chuyển domain model [Notification] thành map dữ liệu để ghi lên Firestore.
     *
     * @return Map gồm các field ổn định, dùng chuỗi rỗng cho giá trị optional để tránh thiếu schema.
     */
    private fun Notification.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            FIELD_ID to id,
            FIELD_USER_ID to userId,
            FIELD_TITLE to title,
            FIELD_SUBTITLE to subtitle,
            FIELD_TYPE to type.name,
            FIELD_RELATED_ID to relatedId.orEmpty(),
            FIELD_IS_READ to isRead,
            FIELD_CREATED_AT to createdAt,
            FIELD_UPDATED_AT to updatedAt,
            FIELD_DEEP_LINK_DESTINATION to deepLinkDestination.orEmpty(),
            FIELD_DEEP_LINK_TARGET_ID to deepLinkTargetId.orEmpty(),
            FIELD_SENDER_ID to senderId.orEmpty(),
            FIELD_GROUP_ID to groupId.orEmpty(),
        )
    }

    /**
     * Loại bỏ các thông báo demo cũ từng được seed trong giai đoạn phát triển.
     *
     * @return Danh sách thông báo thực tế sau khi lọc.
     */
    private fun List<Notification>.withoutLegacyDemoNotifications(): List<Notification> {
        return filterNot { notification -> notification.isLegacyDemoNotification() }
    }

    /**
     * Sắp xếp thông báo theo thứ tự mới nhất trước, dùng `updatedAt` và `id` làm tiêu chí phụ.
     *
     * @return Danh sách thông báo đã sắp xếp.
     */
    private fun List<Notification>.orderedNewestFirst(): List<Notification> {
        return sortedWith(
            compareByDescending<Notification> { it.createdAt }
                .thenByDescending { it.updatedAt }
                .thenByDescending { it.id }
        )
    }

    /**
     * Nhận diện các bản ghi thông báo demo cũ để không hiển thị trong tài khoản thật.
     *
     * @return true nếu thông báo khớp mẫu dữ liệu demo cần ẩn.
     */
    private fun Notification.isLegacyDemoNotification(): Boolean {
        return when {
            type == NotificationType.TRANSACTION_ALERT &&
                relatedId == "c_food" &&
                deepLinkDestination == NotificationDestination.SPENDING_REMINDERS.name &&
                title == "Cảnh báo chi tiêu: Ăn ngoài" &&
                subtitle == "Bạn đã dùng 75% ngân sách 300,000 VND" -> true

            type == NotificationType.PAYMENT_COMPLETED &&
                relatedId == "bill_123" &&
                deepLinkDestination == NotificationDestination.SPLIT_SETTLE.name &&
                title == "John đã thanh toán cho bạn" -> true

            type == NotificationType.BILL_CREATED &&
                relatedId == "bill_456" &&
                deepLinkDestination == NotificationDestination.SPLIT_DETAIL.name &&
                title == "Đã tạo hóa đơn mới" &&
                subtitle == "Kế hoạch chuyến đi cuối tuần - 500,000 VND" -> true

            else -> false
        }
    }

    /**
     * Chuyển callback của Firebase Task thành suspend function để dùng trong coroutine.
     *
     * @return Kết quả của [Task] khi thành công.
     * @throws Exception lỗi Firebase tương ứng nếu task thất bại.
     */
    private suspend fun <T> Task<T>.awaitFirebase(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Tác vụ Firebase thất bại"),
                    )
                }
            }
        }
    }

    companion object {
        private const val COLLECTION_USER_NOTIFICATIONS = "user_notifications"
        private const val COLLECTION_NOTIFICATIONS = "notifications"

        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TITLE = "title"
        private const val FIELD_SUBTITLE = "subtitle"
        private const val FIELD_TYPE = "type"
        private const val FIELD_RELATED_ID = "relatedId"
        private const val FIELD_IS_READ = "isRead"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_DEEP_LINK_DESTINATION = "deepLinkDestination"
        private const val FIELD_DEEP_LINK_TARGET_ID = "deepLinkTargetId"
        private const val FIELD_SENDER_ID = "senderId"
        private const val FIELD_GROUP_ID = "groupId"

        @Volatile
        private var INSTANCE: FirebaseNotificationRepository? = null

        /**
         * Lấy singleton repository cho tầng notification.
         *
         * @param context Context ứng dụng hoặc activity.
         * @return Thực thể [FirebaseNotificationRepository] dùng chung.
         */
        fun getInstance(context: Context): FirebaseNotificationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseNotificationRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
