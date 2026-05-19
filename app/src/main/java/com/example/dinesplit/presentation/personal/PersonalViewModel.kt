package com.example.dinesplit.presentation.personal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.NotificationFactory
import com.example.dinesplit.domain.model.PersonalReminderTrigger
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.SpendingReminder
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
    private val notificationRepository = AppContainer.notificationRepository(application)
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

    private val _reminders = MutableStateFlow<List<SpendingReminder>>(emptyList())
    val reminders: StateFlow<List<SpendingReminder>> = _reminders.asStateFlow()

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
                setError(throwable)
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
                        amountLabel = "0 VND",
                        progress = 0f,
                        isActive = false
                    )
                )
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
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
                setError(throwable)
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
                setError(throwable)
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

    fun addSpendingReminder(
        categoryId: String?,
        categoryName: String,
        budgetAmount: Double,
        threshold: Float,
        reminderType: ReminderType
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                repository.insertSpendingReminder(
                    SpendingReminder(
                        id = UUID.randomUUID().toString(),
                        userId = currentUserId(),
                        categoryId = categoryId,
                        categoryName = categoryName.trim(),
                        budgetAmount = budgetAmount,
                        threshold = threshold,
                        reminderType = reminderType
                    )
                )
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
            }
        }
    }

    fun deleteSpendingReminder(reminderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                repository.deleteSpendingReminder(reminderId)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
            }
        }
    }

    fun checkSpendingReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                syncSpendingReminders(repository.getAllTransactions())
            }.onFailure { throwable ->
                setError(throwable)
            }
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

            syncSpendingReminders(allTransactions)
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
            calendar.get(Calendar.MONTH) + 1 == monthFilter.month &&
                calendar.get(Calendar.YEAR) == monthFilter.year
        }
    }

    private fun buildUiState(
        transactions: List<Transaction>,
        categories: List<StoredCategory>
    ): PersonalUiState {
        val income = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val expense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        return PersonalUiState(
            isLoading = false,
            isSaving = false,
            errorMessage = null,
            currentUserId = currentUserId(),
            transactions = transactions,
            categories = categories,
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense
        )
    }

    private suspend fun syncSpendingReminders(allTransactions: List<Transaction>) {
        val uid = currentUserId()
        if (uid.isEmpty()) {
            _reminders.value = emptyList()
            return
        }

        val reminders = repository.getSpendingReminders()
        val activeReminders = reminders.filter { it.isEnabled }
        if (activeReminders.isEmpty()) {
            _reminders.value = reminders
            return
        }

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonthTransactions = allTransactions.filter { transaction ->
            val transactionCalendar = Calendar.getInstance().apply {
                timeInMillis = transaction.date
            }
            transactionCalendar.get(Calendar.MONTH) + 1 == currentMonth &&
                transactionCalendar.get(Calendar.YEAR) == currentYear
        }
        val spentByCategory = currentMonthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

        activeReminders.forEach { reminder ->
            val spent = if (reminder.categoryId == null) {
                spentByCategory.values.sum()
            } else {
                spentByCategory[reminder.categoryId] ?: 0.0
            }
            val isOverThreshold = spent >= reminder.budgetAmount * reminder.threshold
            val wasNotAlertedToday = reminder.lastAlertedAt?.let { lastAlertedAt ->
                val lastCalendar = Calendar.getInstance().apply {
                    timeInMillis = lastAlertedAt
                }
                lastCalendar.get(Calendar.YEAR) != calendar.get(Calendar.YEAR) ||
                    lastCalendar.get(Calendar.DAY_OF_YEAR) != calendar.get(Calendar.DAY_OF_YEAR)
            } ?: true

            when {
                isOverThreshold && wasNotAlertedToday -> {
                    notificationRepository.insertNotification(
                        NotificationFactory.fromReminderTrigger(
                            PersonalReminderTrigger(
                                categoryId = reminder.categoryId,
                                categoryName = reminder.categoryName,
                                currentSpent = spent,
                                budgetLimit = reminder.budgetAmount,
                                thresholdPercent = reminder.threshold
                            ),
                            uid
                        )
                    )
                    repository.updateSpendingReminder(
                        reminder.copy(
                            currentSpent = spent,
                            lastAlertedAt = System.currentTimeMillis()
                        )
                    )
                }
                spent != reminder.currentSpent -> {
                    repository.updateSpendingReminder(reminder.copy(currentSpent = spent))
                }
            }
        }

        _reminders.value = repository.getSpendingReminders()
    }

    private fun setError(throwable: Throwable) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isSaving = false,
            errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
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
