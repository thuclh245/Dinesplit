package com.example.dinesplit.presentation.personal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
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

    private val _chartState = MutableStateFlow(PersonalChartState())
    val chartState: StateFlow<PersonalChartState> = _chartState.asStateFlow()

    private val _uiState = MutableStateFlow(PersonalUiState())
    val uiState: StateFlow<PersonalUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                repository.insertTransaction(transaction)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                )
            }
        }
    }

    fun addCategory(
        name: String,
        description: String,
        type: TransactionType,
        isCustom: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                repository.insertCategory(
                    StoredCategory(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        icon = iconCodeForName(name),
                        type = type,
                        isCustom = isCustom,
                        description = description.trim(),
                        amountLabel = "0đ",
                        progress = 0f,
                        isActive = false
                    )
                )
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                )
            }
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
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val existing = _categories.value.firstOrNull { it.id == categoryId } ?: return@runCatching
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
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                )
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                repository.deleteCategory(categoryId)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                )
            }
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

    private suspend fun refreshStateInternal(showLoading: Boolean = true) {
        if (showLoading) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                currentUserId = currentUserId()
            )
        }

        runCatching {
            val categories = repository.getCategories()
            val allTransactions = repository.getAllTransactions()
            val filteredTransactions = filterTransactions(
                transactions = allTransactions,
                monthFilter = currentMonthFilter.value
            )

            _transactions.value = filteredTransactions
            _categories.value = categories
            _categoryNamesByType.value = categories
                .groupBy { it.type }
                .mapValues { (_, items) -> items.map { it.name }.sorted() }

            _chartState.value = PersonalChartState(
                pieSlices = filteredTransactions.toPieCategorySlices(),
                dailyExpenseBars = filteredTransactions.toDailyExpenseBars(),
                monthlySummary = filteredTransactions.toMonthlySummary()
            )

            _uiState.value = buildUiState(
                transactions = filteredTransactions,
                categories = categories
            )
        }.onFailure { throwable ->
            _transactions.value = emptyList()
            _categories.value = emptyList()
            _categoryNamesByType.value = emptyMap()
            _chartState.value = PersonalChartState()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isSaving = false,
                currentUserId = currentUserId(),
                errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
            )
        }
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
            isSaving = false,
            errorMessage = null,
            currentUserId = currentUserId(),
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

    private fun currentUserId(): String {
        return FirebaseProviders.auth.currentUser?.uid.orEmpty()
    }

    private fun iconCodeForName(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "OT"
            parts.size == 1 -> parts.first().take(2).uppercase()
            else -> "${parts[0].first()}${parts[1].first()}".uppercase()
        }
    }
}
