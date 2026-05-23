package com.example.dinesplit.data.seeder

import com.example.dinesplit.data.repository.FirebasePersonalRepository
import com.example.dinesplit.data.repository.FirebaseNotificationRepository
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationType
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import java.util.UUID

/**
 * Debug-only seeder to inject demo transactions + notifications.
 * Use only for dev/demo, not in production release.
 *
 * Usage:
 *   viewModelScope.launch(Dispatchers.IO) {
 *       DemoDataSeeder.seedDemoTransactions(personalRepo, userId)
 *       DemoDataSeeder.seedDemoNotifications(notificationRepo, userId)
 *   }
 */
@Suppress("unused", "ObjectName")
object DemoDataSeeder {
    @Suppress("unused")
    suspend fun seedDemoTransactions(
        personalRepo: FirebasePersonalRepository,
        userId: String
    ) {
        val now = System.currentTimeMillis()

        val expenses = listOf(
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = 150000.0,
                type = TransactionType.EXPENSE,
                categoryId = "c_food",
                category = "Dining Out",
                note = "Lunch with team",
                date = now - 1000 * 60 * 60 * 2,
                createdAt = now
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = 50000.0,
                type = TransactionType.EXPENSE,
                categoryId = "c_transit",
                category = "Transit",
                note = "Uber to office",
                date = now - 1000 * 60 * 60 * 24,
                createdAt = now
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = 320000.0,
                type = TransactionType.EXPENSE,
                categoryId = "c_grocery",
                category = "Groceries",
                note = "Weekly shopping",
                date = now - 1000 * 60 * 60 * 48,
                createdAt = now
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = 200000.0,
                type = TransactionType.INCOME,
                categoryId = "c_bonus",
                category = "Bonus",
                note = "Project completion bonus",
                date = now - 1000 * 60 * 60 * 72,
                createdAt = now
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = 100000.0,
                type = TransactionType.EXPENSE,
                categoryId = "c_fun",
                category = "Entertainment",
                note = "Movie tickets",
                date = now - 1000 * 60 * 60 * 96,
                createdAt = now
            )
        )

        expenses.forEach { transaction ->
            try {
                personalRepo.insertTransaction(transaction)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Suppress("unused")
    suspend fun seedDemoNotifications(
        notificationRepo: FirebaseNotificationRepository,
        userId: String
    ) {
        val now = System.currentTimeMillis()
        val notifications = listOf(
            Notification(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "Spending alert: Dining Out",
                subtitle = "You have spent 75% of your 300,000 VND budget",
                type = NotificationType.TRANSACTION_ALERT,
                relatedId = "c_food",
                isRead = false,
                createdAt = now - 1000 * 60 * 30,
                updatedAt = now - 1000 * 60 * 30,
                deepLinkDestination = "SPENDING_REMINDERS",
                deepLinkTargetId = "c_food"
            ),
            Notification(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "John paid you",
                subtitle = "Group dinner split settled - 120,000 VND",
                type = NotificationType.PAYMENT_COMPLETED,
                relatedId = "bill_123",
                isRead = true,
                createdAt = now - 1000 * 60 * 60 * 2,
                updatedAt = now - 1000 * 60 * 60 * 2,
                deepLinkDestination = "SPLIT_SETTLE",
                deepLinkTargetId = "bill_123"
            ),
            Notification(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "New bill created",
                subtitle = "Weekend trip planning - 500,000 VND",
                type = NotificationType.BILL_CREATED,
                relatedId = "bill_456",
                isRead = false,
                createdAt = now - 1000 * 60 * 60 * 3,
                updatedAt = now - 1000 * 60 * 60 * 3,
                deepLinkDestination = "SPLIT_DETAIL",
                deepLinkTargetId = "bill_456"
            )
        )

        notifications.forEach { notification ->
            try {
                notificationRepo.insertNotification(notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

