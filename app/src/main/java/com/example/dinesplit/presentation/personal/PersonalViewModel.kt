package com.example.dinesplit.presentation.personal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class PersonalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppContainer.personalRepository(application)
    private val currentMonthFilter = MutableStateFlow<MonthYearFilter?>(null)

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _categories = MutableStateFlow<List<StoredCategory>>(emptyList())
    val categories: StateFlow<List<StoredCategory>> = _categories.asStateFlow()

    private val _categoryNamesByType = MutableStateFlow<Map<TransactionType, List<String>>>(emptyMap())
    val categoryNamesByType: StateFlow<Map<TransactionType, List<String>>> = _categoryNamesByType.asStateFlow()

    private val _uiState = MutableStateFlow(PersonalUiState())
    val uiState: StateFlow<PersonalUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTransaction(transaction)
            refreshStateInternal()
        }
    }

    fun addCategory(
        name: String,
        description: String,
        type: TransactionType,
        isCustom: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(
                StoredCategory(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    icon = iconCodeForName(name),
                    type = type,
                    isCustom = isCustom,
                    description = description.trim(),
                    amountLabel = "$0.00",
                    progress = 0f,
                    isActive = false
                )
            )
            refreshStateInternal()
        }
    }

    fun updateCategory(
        categoryId: String,
        name: String,
        description: String,
        type: TransactionType,
        isCustom: Boolean,
        isActive: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = _categories.value.firstOrNull { it.id == categoryId } ?: return@launch
            repository.updateCategory(
                existing.copy(
                    name = name.trim(),
                    icon = iconCodeForName(name),
                    type = type,
                    isCustom = isCustom,
                    description = description.trim(),
                    isActive = isActive
                )
            )
            refreshStateInternal()
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(categoryId)
            refreshStateInternal()
        }
    }

    fun filterByMonth(month: Int, year: Int) {
        currentMonthFilter.value = MonthYearFilter(month = month, year = year)
        refreshState()
    }

    fun clearMonthFilter() {
        currentMonthFilter.value = null
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshStateInternal()
        }
    }

    private fun refreshStateInternal() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val allTransactions = repository.getAllTransactions()
        val filteredTransactions = filterTransactions(
            transactions = allTransactions,
            monthFilter = currentMonthFilter.value
        )
        val categories = repository.getCategories()

        _transactions.value = filteredTransactions
        _categories.value = categories
        _categoryNamesByType.value = categories
            .groupBy { it.type }
            .mapValues { (_, items) -> items.map { it.name }.sorted() }

        _uiState.value = buildUiState(
            transactions = filteredTransactions,
            categories = categories
        )
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

    private fun buildUiState(
        transactions: List<Transaction>,
        categories: List<StoredCategory>
    ): PersonalUiState {
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
            categories = categories,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = balance
        )
    }

    private data class MonthYearFilter(
        val month: Int,
        val year: Int
    )

    private fun iconCodeForName(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "OT"
            parts.size == 1 -> parts.first().take(2).uppercase()
            else -> "${parts[0].first()}${parts[1].first()}".uppercase()
        }
    }
}
