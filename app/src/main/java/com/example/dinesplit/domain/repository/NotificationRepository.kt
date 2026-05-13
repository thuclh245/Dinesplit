package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(userId: String): Flow<List<Notification>>
    suspend fun markAsRead(notificationId: String)
    suspend fun deleteNotification(notificationId: String)
}
