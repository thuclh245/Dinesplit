package com.example.dinesplit.presentation.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.SplitNotificationTrigger
import com.example.dinesplit.domain.model.FeedNotificationTrigger
import com.example.dinesplit.domain.model.NotificationFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppContainer.notificationRepository(application)

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        refreshNotifications()
    }

    fun refreshNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            runCatching {
                val notifications = repository.getNotifications()
                _notifications.value = notifications
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentUserId = currentUserId(),
                    unreadCount = notifications.count { !it.isRead }
                )
            }.onFailure { throwable ->
                _notifications.value = emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentUserId = currentUserId(),
                    errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                )
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.markAsRead(notificationId)
                _notifications.value = _notifications.value.map { notif ->
                    if (notif.id == notificationId) notif.copy(isRead = true) else notif
                }
                _uiState.value = _uiState.value.copy(
                    unreadCount = _notifications.value.count { !it.isRead }
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                )
            }
        }
    }

    fun markAsUnread(notificationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.markAsUnread(notificationId)
                _notifications.value = _notifications.value.map { notif ->
                    if (notif.id == notificationId) notif.copy(isRead = false) else notif
                }
                _uiState.value = _uiState.value.copy(
                    unreadCount = _notifications.value.count { !it.isRead }
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                )
            }
        }
    }

    // Tuần 3: Trigger handlers từ B/D
    fun onFeedTrigger(trigger: FeedNotificationTrigger) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val notification = NotificationFactory.fromFeedTrigger(trigger, currentUserId())
                // Lưu vào Firestore (tuần 3+)
                // Giờ chỉ add vào in-memory
                _notifications.value = listOf(notification) + _notifications.value
                _uiState.value = _uiState.value.copy(
                    unreadCount = _notifications.value.count { !it.isRead }
                )
            }
        }
    }

    fun onSplitTrigger(trigger: SplitNotificationTrigger) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val notification = NotificationFactory.fromSplitTrigger(trigger, currentUserId())
                // Lưu vào Firestore (tuần 3+)
                // Giờ chỉ add vào in-memory
                _notifications.value = listOf(notification) + _notifications.value
                _uiState.value = _uiState.value.copy(
                    unreadCount = _notifications.value.count { !it.isRead }
                )
            }
        }
    }

    private fun currentUserId(): String {
        return FirebaseProviders.auth.currentUser?.uid.orEmpty()
    }
}

data class NotificationUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUserId: String = "",
    val unreadCount: Int = 0
)

