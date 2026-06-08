package com.example.dinesplit.presentation.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.FeedNotificationTrigger
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationFactory
import com.example.dinesplit.domain.model.PersonalNotificationTrigger
import com.example.dinesplit.domain.model.SplitNotificationTrigger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel điều phối màn hình thông báo.
 *
 * Lớp này theo dõi trạng thái đăng nhập Firebase, tự động đăng ký/hủy đăng ký listener Firestore
 * theo từng người dùng, giữ danh sách thông báo mới nhất và cập nhật số lượng chưa đọc cho UI.
 */
class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppContainer.notificationRepository(application)

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var observeJob: Job? = null
    private var lastObservedUserId: String? = null

    init {
        setupAuthStateListener()
        handleAuthUserChanged(currentUserId().takeIf { it.isNotBlank() })
    }

    /**
     * Đăng ký listener Firebase Auth để đổi nguồn dữ liệu thông báo khi người dùng đăng nhập/đăng xuất.
     */
    private fun setupAuthStateListener() {
        authStateListener =
            FirebaseAuth.AuthStateListener { auth ->
                handleAuthUserChanged(auth.currentUser?.uid)
            }
        FirebaseProviders.auth.addAuthStateListener(authStateListener!!)
    }

    /**
     * Xử lý khi UID hiện tại thay đổi.
     *
     * Nếu không còn người dùng, ViewModel xóa state hiện tại. Nếu có người dùng mới, ViewModel
     * hủy listener cũ và bắt đầu observe thông báo theo UID mới.
     *
     * @param userId UID mới từ Firebase Auth, hoặc null khi đăng xuất.
     */
    private fun handleAuthUserChanged(userId: String?) {
        if (userId.isNullOrBlank()) {
            observeJob?.cancel()
            lastObservedUserId = null
            clearNotificationState()
            return
        }

        if (userId != lastObservedUserId || _uiState.value.currentUserId != userId) {
            observeJob?.cancel()
            lastObservedUserId = userId
            clearNotificationState(currentUserId = userId, isLoading = true)
            observeNotifications(expectedUserId = userId)
        }
    }

    /**
     * Đặt lại danh sách thông báo và trạng thái UI về trạng thái sạch.
     *
     * @param currentUserId UID đang được giữ trong state sau khi reset.
     * @param isLoading Có hiển thị trạng thái tải lại hay không.
     */
    private fun clearNotificationState(
        currentUserId: String = "",
        isLoading: Boolean = false,
    ) {
        _notifications.value = emptyList()
        _uiState.value =
            NotificationUiState(
                isLoading = isLoading,
                currentUserId = currentUserId,
            )
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { listener ->
            FirebaseProviders.auth.removeAuthStateListener(listener)
        }
        observeJob?.cancel()
    }

    /**
     * Lắng nghe thông báo realtime từ repository cho đúng người dùng mong đợi.
     *
     * @param expectedUserId UID tại thời điểm bắt đầu observe; dùng để bỏ qua callback muộn sau khi đổi tài khoản.
     */
    private fun observeNotifications(expectedUserId: String) {
        observeJob =
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = true,
                        errorMessage = null,
                        currentUserId = expectedUserId,
                    )
                repository.observeNotifications()
                    .catch { throwable ->
                        if (!isCurrentUser(expectedUserId)) return@catch
                        _notifications.value = emptyList()
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                currentUserId = expectedUserId,
                                errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                            )
                    }
                    .collectLatest { notifications ->
                        if (!isCurrentUser(expectedUserId)) return@collectLatest
                        val orderedNotifications =
                            notifications
                                .filter { notification -> notification.userId == expectedUserId }
                                .orderedNewestFirst()
                        _notifications.value = orderedNotifications
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                errorMessage = null,
                                currentUserId = expectedUserId,
                                unreadCount = orderedNotifications.count { !it.isRead },
                            )
                    }
            }
    }

    /**
     * Tải lại danh sách thông báo một lần từ repository.
     *
     * Hàm này dùng cho thao tác refresh thủ công; realtime listener vẫn tiếp tục hoạt động song song.
     */
    fun refreshNotifications() {
        val expectedUserId = currentUserId()
        if (expectedUserId.isBlank()) {
            clearNotificationState()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    currentUserId = expectedUserId,
                )

            runCatching {
                val notifications =
                    repository.getNotifications()
                        .filter { notification -> notification.userId == expectedUserId }
                        .orderedNewestFirst()
                if (!isCurrentUser(expectedUserId)) return@runCatching

                _notifications.value = notifications
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        currentUserId = expectedUserId,
                        unreadCount = notifications.count { !it.isRead },
                    )
            }.onFailure { throwable ->
                if (!isCurrentUser(expectedUserId)) return@onFailure
                _notifications.value = emptyList()
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        currentUserId = expectedUserId,
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
        }
    }

    /**
     * Đánh dấu một thông báo là đã đọc.
     *
     * @param notificationId ID thông báo cần cập nhật.
     */
    fun markAsRead(notificationId: String) {
        updateNotificationReadState(notificationId = notificationId, isRead = true)
    }

    /**
     * Đánh dấu một thông báo là chưa đọc.
     *
     * @param notificationId ID thông báo cần cập nhật.
     */
    fun markAsUnread(notificationId: String) {
        updateNotificationReadState(notificationId = notificationId, isRead = false)
    }

    /**
     * Đánh dấu toàn bộ thông báo chưa đọc của người dùng hiện tại thành đã đọc.
     */
    fun markAllAsRead() {
        val expectedUserId = currentUserId()
        if (expectedUserId.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (!isCurrentUser(expectedUserId)) return@runCatching

                val unreadNotifications =
                    _notifications.value.filter { notification ->
                        !notification.isRead && notification.userId == expectedUserId
                    }
                unreadNotifications.forEach { notification ->
                    if (!isCurrentUser(expectedUserId)) return@runCatching
                    repository.markAsRead(notification.id)
                }
                if (!isCurrentUser(expectedUserId)) return@runCatching

                _notifications.value =
                    _notifications.value.map { notification ->
                        if (notification.userId == expectedUserId) {
                            notification.copy(isRead = true)
                        } else {
                            notification
                        }
                    }.orderedNewestFirst()
                _uiState.value = _uiState.value.copy(unreadCount = 0)
            }.onFailure { throwable ->
                if (!isCurrentUser(expectedUserId)) return@onFailure
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
        }
    }

    /**
     * Nhận trigger từ module Feed và tạo thông báo tương ứng cho người dùng hiện tại.
     *
     * @param trigger Mô tả sự kiện feed như like/comment/follow.
     */
    fun onFeedTrigger(trigger: FeedNotificationTrigger) {
        val userId = currentUserId()
        if (userId.isBlank()) return

        insertGeneratedNotification(
            notification = NotificationFactory.fromFeedTrigger(trigger, userId),
        )
    }

    /**
     * Nhận trigger từ module Split và tạo thông báo về bill/thanh toán.
     *
     * @param trigger Mô tả sự kiện split như tạo bill, thanh toán hoặc settle.
     */
    fun onSplitTrigger(trigger: SplitNotificationTrigger) {
        val userId = currentUserId()
        if (userId.isBlank()) return

        insertGeneratedNotification(
            notification = NotificationFactory.fromSplitTrigger(trigger, userId),
        )
    }

    /**
     * Nhận trigger từ module tài chính cá nhân và tạo thông báo cảnh báo/ngân sách.
     *
     * @param trigger Mô tả sự kiện cá nhân như reminder vượt ngưỡng.
     */
    fun onPersonalTrigger(trigger: PersonalNotificationTrigger) {
        val userId = currentUserId()
        if (userId.isBlank()) return

        insertGeneratedNotification(
            notification = NotificationFactory.fromPersonalTrigger(trigger, userId),
        )
    }

    /**
     * Cập nhật trạng thái đọc/chưa đọc của một thông báo trên Firestore và state cục bộ.
     *
     * @param notificationId ID thông báo cần cập nhật.
     * @param isRead Trạng thái đọc mới.
     */
    private fun updateNotificationReadState(
        notificationId: String,
        isRead: Boolean,
    ) {
        val expectedUserId = currentUserId()
        if (expectedUserId.isBlank()) return
        if (_notifications.value.none { it.id == notificationId && it.userId == expectedUserId }) return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (!isCurrentUser(expectedUserId)) return@runCatching

                if (isRead) {
                    repository.markAsRead(notificationId)
                } else {
                    repository.markAsUnread(notificationId)
                }
                if (!isCurrentUser(expectedUserId)) return@runCatching

                _notifications.value =
                    _notifications.value.map { notification ->
                        if (notification.id == notificationId && notification.userId == expectedUserId) {
                            notification.copy(isRead = isRead)
                        } else {
                            notification
                        }
                    }.orderedNewestFirst()
                _uiState.value =
                    _uiState.value.copy(
                        unreadCount = _notifications.value.count { !it.isRead },
                    )
            }.onFailure { throwable ->
                if (!isCurrentUser(expectedUserId)) return@onFailure
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
        }
    }

    /**
     * Ghi thông báo được tạo từ trigger vào repository và đẩy ngay vào danh sách cục bộ.
     *
     * @param notification Thông báo đã được [NotificationFactory] tạo ra.
     */
    private fun insertGeneratedNotification(notification: Notification) {
        val expectedUserId = notification.userId.ifBlank { currentUserId() }
        if (expectedUserId.isBlank() || !isCurrentUser(expectedUserId)) return
        val normalizedNotification =
            if (notification.userId.isBlank()) {
                notification.copy(userId = expectedUserId)
            } else {
                notification
            }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (!isCurrentUser(expectedUserId)) return@runCatching

                repository.insertNotification(normalizedNotification)
                if (!isCurrentUser(expectedUserId)) return@runCatching

                _notifications.value = (listOf(normalizedNotification) + _notifications.value).orderedNewestFirst()
                _uiState.value =
                    _uiState.value.copy(
                        unreadCount = _notifications.value.count { !it.isRead },
                    )
            }.onFailure { throwable ->
                if (!isCurrentUser(expectedUserId)) return@onFailure
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
        }
    }

    /**
     * Lấy UID người dùng đang đăng nhập hiện tại.
     *
     * @return UID hoặc chuỗi rỗng nếu chưa đăng nhập.
     */
    private fun currentUserId(): String {
        return FirebaseProviders.auth.currentUser?.uid.orEmpty()
    }

    /**
     * Kiểm tra callback hiện tại còn thuộc đúng người dùng đang đăng nhập hay không.
     *
     * @param expectedUserId UID đã chốt khi bắt đầu tác vụ bất đồng bộ.
     * @return true nếu UID vẫn khớp, false nếu người dùng đã đổi/đăng xuất.
     */
    private fun isCurrentUser(expectedUserId: String): Boolean {
        return expectedUserId.isNotBlank() && currentUserId() == expectedUserId
    }

    /**
     * Sắp xếp thông báo mới nhất trước.
     *
     * @return Danh sách đã sắp theo `createdAt`, `updatedAt`, rồi `id` giảm dần.
     */
    private fun List<Notification>.orderedNewestFirst(): List<Notification> {
        return sortedWith(
            compareByDescending<Notification> { it.createdAt }
                .thenByDescending { it.updatedAt }
                .thenByDescending { it.id }
        )
    }
}

/**
 * State bất biến cho màn hình thông báo.
 *
 * @property isLoading UI đang tải dữ liệu hay không.
 * @property errorMessage Thông báo lỗi thân thiện với người dùng.
 * @property currentUserId UID mà state hiện tại đang đại diện.
 * @property unreadCount Số lượng thông báo chưa đọc.
 */
data class NotificationUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUserId: String = "",
    val unreadCount: Int = 0,
)
