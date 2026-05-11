package com.example.dinesplit.domain.repository

import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.Transaction

interface PersonalRepository {
    fun getAllTransactions(): List<Transaction>
    fun getCategories(): List<StoredCategory>
    fun insertTransaction(transaction: Transaction)
    fun insertCategory(category: StoredCategory)
    fun updateCategory(category: StoredCategory)
    fun deleteCategory(categoryId: String)
}
