package com.example.dinesplit.presentation.personal

import androidx.lifecycle.ViewModel
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class PersonalViewModel : ViewModel() {
    private val allTransactions = MutableStateFlow(seedTransactions())
    private val currentMonthFilter = MutableStateFlow<MonthYearFilter?>(null)

    private val _transactions = MutableStateFlow(seedTransactions())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _uiState = MutableStateFlow(PersonalUiState())
    val uiState: StateFlow<PersonalUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun addTransaction(transaction: Transaction) {
        allTransactions.value = allTransactions.value + transaction
        refreshState()
    }

    fun filterByMonth(month: Int, year: Int) {
        currentMonthFilter.value = MonthYearFilter(month = month, year = year)
        refreshState()
    }

    fun clearMonthFilter() {
        currentMonthFilter.value = null
        refreshState()
    }

    private fun refreshState() {
        val filteredTransactions = filterTransactions(
            transactions = allTransactions.value,
            monthFilter = currentMonthFilter.value
        )

        _transactions.value = filteredTransactions
        _uiState.value = buildUiState(filteredTransactions)
    }

    private fun filterTransactions(
        transactions: List<Transaction>,
        monthFilter: MonthYearFilter?
    ): List<Transaction> {
        if (monthFilter == null) return transactions

        return transactions.filter { transaction ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = transaction.date
            }
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            month == monthFilter.month && year == monthFilter.year
        }
    }

    private fun buildUiState(transactions: List<Transaction>): PersonalUiState {
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val balance = totalIncome - totalExpense

        return PersonalUiState(
            isLoading = false,
            transactions = transactions,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = balance
        )
    }

    private fun seedTransactions(): List<Transaction> {
        return listOf(
            Transaction(
                id = "tx_1",
                userId = "user_1",
                amount = 525000.0,
                type = TransactionType.EXPENSE,
                category = "Food",
                note = "Lunch with team",
                date = epochMillis(year = 2026, month = 4, day = 5),
                createdAt = epochMillis(year = 2026, month = 4, day = 5)
            ),
            Transaction(
                id = "tx_2",
                userId = "user_1",
                amount = 187500.0,
                type = TransactionType.EXPENSE,
                category = "Travel",
                note = null,
                date = epochMillis(year = 2026, month = 4, day = 4),
                createdAt = epochMillis(year = 2026, month = 4, day = 4)
            ),
            Transaction(
                id = "tx_3",
                userId = "user_1",
                amount = 3500000.0,
                type = TransactionType.INCOME,
                category = "Salary",
                note = "Monthly salary",
                date = epochMillis(year = 2026, month = 4, day = 1),
                createdAt = epochMillis(year = 2026, month = 4, day = 1)
            ),
            Transaction(
                id = "tx_4",
                userId = "user_1",
                amount = 220000.0,
                type = TransactionType.EXPENSE,
                category = "Drink",
                note = null,
                date = epochMillis(year = 2026, month = 3, day = 20),
                createdAt = epochMillis(year = 2026, month = 3, day = 20)
            ),
            Transaction(
                id = "tx_5",
                userId = "user_1",
                amount = 750000.0,
                type = TransactionType.INCOME,
                category = "Bonus",
                note = "Project reward",
                date = epochMillis(year = 2026, month = 3, day = 15),
                createdAt = epochMillis(year = 2026, month = 3, day = 15)
            )
        )
    }

    private fun epochMillis(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private data class MonthYearFilter(
        val month: Int,
        val year: Int
    )
}

