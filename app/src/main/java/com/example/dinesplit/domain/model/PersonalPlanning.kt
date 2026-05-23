package com.example.dinesplit.domain.model

enum class RecurringCadence {
    WEEKLY,
    MONTHLY
}

data class RecurringRule(
    val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: String,
    val categoryName: String,
    val cadence: RecurringCadence,
    val dayOfMonth: Int,
    val nextRunAt: Long,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    PAUSED
}

data class PersonalGoal(
    val id: String,
    val userId: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val categoryId: String?,
    val deadlineAt: Long,
    val status: GoalStatus,
    val createdAt: Long,
    val updatedAt: Long
)

enum class WalletType {
    CASH,
    BANK,
    EWALLET,
    CREDIT
}

data class PersonalWallet(
    val id: String,
    val userId: String,
    val name: String,
    val type: WalletType,
    val balance: Double,
    val color: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
