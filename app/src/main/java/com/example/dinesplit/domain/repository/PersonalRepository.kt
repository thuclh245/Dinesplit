package com.example.dinesplit.domain.repository

import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.SpendingReminder
import com.example.dinesplit.domain.model.Transaction

interface PersonalRepository {
    suspend fun getAllTransactions(): List<Transaction>
    suspend fun getCategories(): List<StoredCategory>
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun insertCategory(category: StoredCategory)
    suspend fun updateCategory(category: StoredCategory)
    suspend fun deleteCategory(categoryId: String)

    suspend fun getSpendingReminders(): List<SpendingReminder>
    suspend fun insertSpendingReminder(reminder: SpendingReminder)
    suspend fun updateSpendingReminder(reminder: SpendingReminder)
    suspend fun deleteSpendingReminder(reminderId: String)
}
