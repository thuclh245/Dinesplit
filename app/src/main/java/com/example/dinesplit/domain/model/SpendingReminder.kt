package com.example.dinesplit.domain.model

/**
 * Spending reminder model for week 2+.
 * Can be stored locally or synced to Firebase.
 * Supports budget alerts and spending milestones.
 */
data class SpendingReminder(
    val id: String,
    val userId: String,
    val categoryId: String? = null,  // null = overall budget
    val categoryName: String = "Overall",
    val budgetAmount: Double,
    val currentSpent: Double = 0.0,
    val threshold: Float = 0.8f,  // Alert at 80% of budget
    val reminderType: ReminderType = ReminderType.WEEKLY,
    val isEnabled: Boolean = true,
    val lastAlertedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ReminderType {
    DAILY,
    WEEKLY,
    MONTHLY,
    MILESTONE  // Alert on specific spending amount
}

