package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(): Flow<List<Notification>>

    suspend fun getNotifications(): List<Notification>

    suspend fun insertNotification(notification: Notification)

    suspend fun markAsRead(notificationId: String)

    suspend fun markAsUnread(notificationId: String)
}
