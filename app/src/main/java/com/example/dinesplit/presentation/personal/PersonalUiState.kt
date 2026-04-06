package com.example.dinesplit.presentation.personal

import com.example.dinesplit.domain.model.Transaction

data class PersonalUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0
)

