package com.example.dinesplit.presentation.personal

import com.example.dinesplit.data.repository.StoredCategory
import com.example.dinesplit.domain.model.Transaction

data class PersonalUiState(
    val isLoading: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val categories: List<StoredCategory> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0
)

