package com.example.dinesplit.domain.repository

import android.net.Uri
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.PersonalGoal
import com.example.dinesplit.domain.model.PersonalWallet
import com.example.dinesplit.domain.model.RecurringRule
import com.example.dinesplit.domain.model.SpendingReminder
import com.example.dinesplit.domain.model.Transaction

interface PersonalRepository {
    suspend fun getAllTransactions(): List<Transaction>

    suspend fun getCategories(): List<StoredCategory>

    suspend fun insertTransaction(transaction: Transaction)

    suspend fun updateTransaction(transaction: Transaction)

    suspend fun deleteTransaction(transactionId: String)

    suspend fun uploadReceiptImage(
        transactionId: String,
        receiptUri: Uri,
    ): Result<String>

    suspend fun insertCategory(category: StoredCategory)

    suspend fun updateCategory(category: StoredCategory)

    suspend fun deleteCategory(categoryId: String)

    suspend fun getSpendingReminders(): List<SpendingReminder>

    suspend fun insertSpendingReminder(reminder: SpendingReminder)

    suspend fun updateSpendingReminder(reminder: SpendingReminder)

    suspend fun deleteSpendingReminder(reminderId: String)

    suspend fun getRecurringRules(): List<RecurringRule>

    suspend fun insertRecurringRule(rule: RecurringRule)

    suspend fun updateRecurringRule(rule: RecurringRule)

    suspend fun deleteRecurringRule(ruleId: String)

    suspend fun getGoals(): List<PersonalGoal>

    suspend fun insertGoal(goal: PersonalGoal)

    suspend fun updateGoal(goal: PersonalGoal)

    suspend fun deleteGoal(goalId: String)

    suspend fun getWallets(): List<PersonalWallet>

    suspend fun insertWallet(wallet: PersonalWallet)

    suspend fun updateWallet(wallet: PersonalWallet)

    suspend fun deleteWallet(walletId: String)
}
