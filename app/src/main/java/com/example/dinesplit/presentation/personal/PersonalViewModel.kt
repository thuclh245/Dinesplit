package com.example.dinesplit.presentation.personal

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.GoalStatus
import com.example.dinesplit.domain.model.NotificationFactory
import com.example.dinesplit.domain.model.PersonalGoal
import com.example.dinesplit.domain.model.PersonalNotificationTrigger
import com.example.dinesplit.domain.model.PersonalWallet
import com.example.dinesplit.domain.model.PersonalReminderTrigger
import com.example.dinesplit.domain.model.PersonalTriggerType
import com.example.dinesplit.domain.model.RecurringCadence
import com.example.dinesplit.domain.model.RecurringRule
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.SpendingReminder
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionSource
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.model.WalletType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

// ...existing code...

class PersonalViewModel(application: Application) : AndroidViewModel(application) {
    // ...existing code...
    
    private var lastUpdateCategoryTime = 0L
    private val minUpdateIntervalMs = 500L  // Debounce: min 500ms between updates
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

    private val _recurringRules = MutableStateFlow<List<RecurringRule>>(emptyList())
    val recurringRules: StateFlow<List<RecurringRule>> = _recurringRules.asStateFlow()

    private val _goals = MutableStateFlow<List<PersonalGoal>>(emptyList())
    val goals: StateFlow<List<PersonalGoal>> = _goals.asStateFlow()

    private val _wallets = MutableStateFlow<List<PersonalWallet>>(emptyList())
    val wallets: StateFlow<List<PersonalWallet>> = _wallets.asStateFlow()

    init {
        refreshState()
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val preparedTransaction = transaction.withUploadedReceiptIfNeeded()
                repository.insertTransaction(preparedTransaction)
                
                // Trigger notification for transaction added
                val notification = NotificationFactory.transactionAdded(
                    amount = preparedTransaction.amount,
                    categoryName = preparedTransaction.category,
                    type = preparedTransaction.type,
                    userId = currentUserId(),
                    transactionId = preparedTransaction.id
                )
                notificationRepository.insertNotification(notification)
                
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
            }
        }
    }

    fun addSplitBillTransaction(bill: Bill) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val uid = currentUserId()
                if (uid.isBlank()) return@runCatching

                val categories = _categories.value.ifEmpty { repository.getCategories() }
                val category = categories.firstOrNull { it.type == TransactionType.EXPENSE && it.id == "c_food" }
                    ?: categories.firstOrNull { it.type == TransactionType.EXPENSE }
                    ?: return@runCatching
                val amount = when {
                    bill.payerId == uid -> bill.totalAmount
                    bill.shares[uid] != null -> bill.shares[uid].orZero()
                    else -> 0.0
                }
                if (amount <= 0.0) return@runCatching

                val splitTransaction = Transaction(
                    id = "split_${bill.id}",
                    userId = uid,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    categoryId = category.id,
                    category = category.name,
                    note = "Split bill: ${bill.name}",
                    date = bill.date,
                    createdAt = System.currentTimeMillis(),
                    source = TransactionSource.SPLIT,
                    sourceGroupId = bill.groupId,
                    sourceBillId = bill.id
                )

                repository.insertTransaction(splitTransaction)
                notificationRepository.insertNotification(
                    NotificationFactory.fromPersonalTrigger(
                        PersonalNotificationTrigger(
                            relatedId = splitTransaction.id,
                            label = bill.name,
                            amount = amount,
                            categoryName = category.name,
                            triggerType = PersonalTriggerType.SPLIT_BRIDGED_TO_PERSONAL
                        ),
                        uid
                    )
                )
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

                // Trigger notification for category created
                val notification = NotificationFactory.categoryCreated(
                    categoryName = name,
                    type = type,
                    userId = currentUserId()
                )
                notificationRepository.insertNotification(notification)

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

                // Check if type changed (EXPENSE <-> INCOME)
                val typeChanged = existing.type != type

                // Update category
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
                
                // If type changed, update all transactions with this category
                if (typeChanged) {
                    val allTransactions = repository.getAllTransactions()
                    val transactionsToUpdate = allTransactions.filter { it.categoryId == categoryId }
                    transactionsToUpdate.forEach { transaction ->
                        repository.updateTransaction(
                            transaction.copy(type = type)
                        )
                    }
                }

                // Always do full refresh after category update (whether type changed or not)
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

                // Trigger notification for reminder created
                val notification = NotificationFactory.reminderCreated(
                    categoryName = categoryName,
                    budgetAmount = budgetAmount,
                    userId = currentUserId()
                )
                notificationRepository.insertNotification(notification)

                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
            }
        }
    }

    fun addRecurringRule(
        name: String,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        categoryName: String,
        cadence: RecurringCadence,
        dayOfMonth: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val now = System.currentTimeMillis()
                val uid = currentUserId()
                val rule = RecurringRule(
                    id = UUID.randomUUID().toString(),
                    userId = uid,
                    name = name.trim(),
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    cadence = cadence,
                    dayOfMonth = dayOfMonth.coerceIn(1, 31),
                    nextRunAt = nextMonthlyRunAt(dayOfMonth),
                    isEnabled = true,
                    createdAt = now,
                    updatedAt = now
                )
                repository.insertRecurringRule(rule)
                notificationRepository.insertNotification(
                    NotificationFactory.fromPersonalTrigger(
                        PersonalNotificationTrigger(
                            relatedId = rule.id,
                            label = rule.name,
                            amount = rule.amount,
                            categoryName = rule.categoryName,
                            triggerType = PersonalTriggerType.RECURRING_RULE_CREATED
                        ),
                        uid
                    )
                )
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    fun deleteRecurringRule(ruleId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.deleteRecurringRule(ruleId)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    fun addGoal(
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        categoryId: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val now = System.currentTimeMillis()
                val uid = currentUserId()
                val goal = PersonalGoal(
                    id = UUID.randomUUID().toString(),
                    userId = uid,
                    title = title.trim(),
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    categoryId = categoryId,
                    deadlineAt = endOfCurrentMonth(),
                    status = GoalStatus.ACTIVE,
                    createdAt = now,
                    updatedAt = now
                )
                repository.insertGoal(goal)
                notificationRepository.insertNotification(
                    NotificationFactory.fromPersonalTrigger(
                        PersonalNotificationTrigger(
                            relatedId = goal.id,
                            label = goal.title,
                            amount = goal.targetAmount,
                            triggerType = PersonalTriggerType.GOAL_CREATED
                        ),
                        uid
                    )
                )
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.deleteGoal(goalId)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    fun addWallet(
        name: String,
        type: WalletType,
        balance: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val now = System.currentTimeMillis()
                val uid = currentUserId()
                val wallet = PersonalWallet(
                    id = UUID.randomUUID().toString(),
                    userId = uid,
                    name = name.trim(),
                    type = type,
                    balance = balance,
                    color = "#AB2D00",
                    isArchived = false,
                    createdAt = now,
                    updatedAt = now
                )
                repository.insertWallet(wallet)
                notificationRepository.insertNotification(
                    NotificationFactory.fromPersonalTrigger(
                        PersonalNotificationTrigger(
                            relatedId = wallet.id,
                            label = wallet.name,
                            amount = wallet.balance,
                            triggerType = PersonalTriggerType.WALLET_CREATED
                        ),
                        uid
                    )
                )
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    fun deleteWallet(walletId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.deleteWallet(walletId)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
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

    // Lightweight refresh for category-only updates (avoids full transaction reload)
    private suspend fun refreshCategoriesOnly() {
        runCatching {
            val categories = repository.getCategories()
            _categories.value = categories
            _categoryNamesByType.value = categories
                .groupBy { it.type }
                .mapValues { (_, items) -> items.map { it.name }.sorted() }

            // Update UI state without reloading transactions
            _uiState.value = buildUiState(
                transactions = _transactions.value,
                categories = categories
            )
        }.onFailure { throwable ->
            setError(throwable)
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
            val recurringRules = repository.getRecurringRules()
            val goals = repository.getGoals()
            val wallets = repository.getWallets()
            val allTransactions = repository.getAllTransactions()

            // Memory Optimization: Load current month transactions first
            // Only load all transactions when necessary (for reminders)
            val monthFilter = currentMonthFilter.value
            val filteredTransactions = if (monthFilter != null) {
                // Load only specific month transactions for filtering
                filterTransactions(
                    transactions = allTransactions,
                    monthFilter = monthFilter
                )
                    .take(500)  // Limit to 500 most recent in this month
            } else {
                // If no month filter, load current month only
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)

                allTransactions.filter { transaction ->
                    val txnCalendar = Calendar.getInstance().apply {
                        timeInMillis = transaction.date
                    }
                    txnCalendar.get(Calendar.MONTH) + 1 == currentMonth &&
                    txnCalendar.get(Calendar.YEAR) == currentYear
                }
                    .take(500)  // Limit to 500 most recent
            }
            val upcomingRecurringExpense = recurringRules
                .filter { it.isEnabled && it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            val savingsGoal = goals
                .filter { it.status == GoalStatus.ACTIVE }
                .sumOf { (it.targetAmount - it.currentAmount).coerceAtLeast(0.0) }
                .coerceAtMost(5_000_000.0)

            _transactions.value = filteredTransactions
            _categories.value = categories
            _recurringRules.value = recurringRules
            _goals.value = goals
            _wallets.value = wallets
            _categoryNamesByType.value = categories
                .groupBy { it.type }
                .mapValues { (_, items) -> items.map { it.name }.sorted() }

            _chartState.value = PersonalChartState(
                pieSlices = filteredTransactions.toPieCategorySlices(),
                dailyExpenseBars = filteredTransactions.toDailyExpenseBars(),
                monthlySummary = filteredTransactions.toMonthlySummary(),
                insights = allTransactions.toMonthlyInsights(),
                safeToSpend = allTransactions.toSafeToSpendForecast(
                    upcomingRecurringExpense = upcomingRecurringExpense,
                    savingsGoal = savingsGoal
                )
            )

            _uiState.value = buildUiState(
                transactions = filteredTransactions,
                categories = categories,
                recurringRules = recurringRules,
                goals = goals,
                wallets = wallets
            )

            // Load all transactions asynchronously for reminders (background)
            viewModelScope.launch(Dispatchers.IO) {
                syncSpendingReminders(allTransactions)
            }
        }.onFailure { throwable ->
            _transactions.value = emptyList()
            _categories.value = emptyList()
            _recurringRules.value = emptyList()
            _goals.value = emptyList()
            _wallets.value = emptyList()
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
        categories: List<StoredCategory>,
        recurringRules: List<RecurringRule> = _recurringRules.value,
        goals: List<PersonalGoal> = _goals.value,
        wallets: List<PersonalWallet> = _wallets.value
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
            recurringRules = recurringRules,
            goals = goals,
            wallets = wallets,
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

    private suspend fun Transaction.withUploadedReceiptIfNeeded(): Transaction {
        val receiptValue = receiptImageUrl?.takeIf { it.isNotBlank() } ?: return this
        val receiptUri = Uri.parse(receiptValue)
        val isLocalReceipt = receiptUri.scheme == "content" || receiptUri.scheme == "file"
        if (!isLocalReceipt) return this

        val uploadedUrl = repository.uploadReceiptImage(id, receiptUri).getOrThrow()
        return copy(
            receiptImageUrl = uploadedUrl,
            source = if (source == TransactionSource.MANUAL) TransactionSource.RECEIPT else source
        )
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

    private fun nextMonthlyRunAt(dayOfMonth: Int): Long {
        val calendar = Calendar.getInstance()
        val targetDay = dayOfMonth.coerceIn(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.DAY_OF_MONTH, targetDay)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.MONTH, 1)
            calendar.set(
                Calendar.DAY_OF_MONTH,
                dayOfMonth.coerceIn(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            )
        }
        return calendar.timeInMillis
    }

    private fun endOfCurrentMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private fun Double?.orZero(): Double = this ?: 0.0
}
