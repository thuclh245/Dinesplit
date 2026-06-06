package com.example.dinesplit.presentation.personal

import android.app.Application
import androidx.core.net.toUri
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
import com.example.dinesplit.domain.model.PersonalReminderTrigger
import com.example.dinesplit.domain.model.PersonalTriggerType
import com.example.dinesplit.domain.model.PersonalWallet
import com.example.dinesplit.domain.model.RecurringCadence
import com.example.dinesplit.domain.model.RecurringRule
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.SpendingReminder
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionSource
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.model.WalletType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

// ...existing code...

class PersonalViewModel(application: Application) : AndroidViewModel(application) {
    // ...existing code...

    private val repository = AppContainer.personalRepository(application)
    private val notificationRepository = AppContainer.notificationRepository(application)
    private val splitRepository = AppContainer.splitRepository(application)
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val allTransactions: StateFlow<List<Transaction>> = _allTransactions.asStateFlow()

    private val _categories = MutableStateFlow<List<StoredCategory>>(emptyList())

    private val _chartState = MutableStateFlow(PersonalChartState())
    val chartState: StateFlow<PersonalChartState> = _chartState.asStateFlow()

    private val _uiState = MutableStateFlow(PersonalUiState())
    val uiState: StateFlow<PersonalUiState> = _uiState.asStateFlow()

    private val _reminders = MutableStateFlow<List<SpendingReminder>>(emptyList())
    val reminders: StateFlow<List<SpendingReminder>> = _reminders.asStateFlow()

    private val _recurringRules = MutableStateFlow<List<RecurringRule>>(emptyList())

    private val _goals = MutableStateFlow<List<PersonalGoal>>(emptyList())

    private val _wallets = MutableStateFlow<List<PersonalWallet>>(emptyList())

    private var lastLoadedUserId: String? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var refreshJob: Job? = null

    init {
        setupAuthStateListener()
        handleAuthUserChanged(currentUserId().takeIf { it.isNotBlank() })
    }

    private fun setupAuthStateListener() {
        authStateListener =
            FirebaseAuth.AuthStateListener { auth ->
                handleAuthUserChanged(auth.currentUser?.uid)
            }
        FirebaseProviders.auth.addAuthStateListener(authStateListener!!)
    }

    private fun handleAuthUserChanged(userId: String?) {
        if (userId.isNullOrBlank()) {
            refreshJob?.cancel()
            lastLoadedUserId = null
            clearAllData()
            return
        }

        if (userId != lastLoadedUserId || _uiState.value.currentUserId != userId) {
            refreshJob?.cancel()
            lastLoadedUserId = userId
            clearAllData(currentUserId = userId, isLoading = true)
            refreshState()
        }
    }

    private fun clearAllData(
        currentUserId: String = "",
        isLoading: Boolean = false,
    ) {
        _transactions.value = emptyList()
        _allTransactions.value = emptyList()
        _categories.value = emptyList()
        _chartState.value = PersonalChartState()
        _reminders.value = emptyList()
        _recurringRules.value = emptyList()
        _goals.value = emptyList()
        _wallets.value = emptyList()
        _uiState.value =
            PersonalUiState(
                isLoading = isLoading,
                currentUserId = currentUserId,
            )
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let {
            FirebaseProviders.auth.removeAuthStateListener(it)
        }
        refreshJob?.cancel()
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val preparedTransaction = transaction.withUploadedReceiptIfNeeded()
                repository.insertTransaction(preparedTransaction)

                // Update wallet balance if a walletId is specified
                preparedTransaction.walletId?.takeIf { it.isNotBlank() }?.let { walletId ->
                    val walletsList = repository.getWallets()
                    val matchingWallet = walletsList.firstOrNull { it.id == walletId }
                    if (matchingWallet != null) {
                        val newBalance =
                            when (preparedTransaction.type) {
                                TransactionType.EXPENSE -> matchingWallet.balance - preparedTransaction.amount
                                TransactionType.INCOME -> matchingWallet.balance + preparedTransaction.amount
                            }
                        repository.updateWallet(
                            matchingWallet.copy(
                                balance = newBalance,
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                }

                // Kích hoạt thông báo cho giao dịch được thêm
                val notification =
                    NotificationFactory.transactionAdded(
                        amount = preparedTransaction.amount,
                        categoryName = preparedTransaction.category,
                        type = preparedTransaction.type,
                        userId = currentUserId(),
                        transactionId = preparedTransaction.id,
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
                syncSplitBillTransactionForCurrentUser(bill = bill, uid = uid, emitNotification = true)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
            }
        }
    }

    fun removeSplitBillTransaction(
        groupId: String,
        billId: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (groupId.isBlank() || billId.isBlank()) return@runCatching
                repository.deleteTransaction(splitTransactionId(groupId, billId))
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
            }
        }
    }

    fun reconcileSplitBillTransaction(bill: Bill) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val uid = currentUserId()
                if (uid.isBlank()) return@runCatching
                syncSplitBillTransactionForCurrentUser(bill = bill, uid = uid, emitNotification = false)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
            }
        }
    }

    private suspend fun syncSplitBillTransactionForCurrentUser(
        bill: Bill,
        uid: String,
        emitNotification: Boolean = false,
    ) {
        val transactionId = splitTransactionId(bill.groupId, bill.id)
        val legacyTransactionId = "split_${bill.id}"
        val amount = bill.shares[uid] ?: 0.0
        if (amount <= 0.0) {
            repository.deleteTransaction(transactionId)
            if (legacyTransactionId != transactionId) {
                repository.deleteTransaction(legacyTransactionId)
            }
            return
        }

        val categories = _categories.value.ifEmpty { repository.getCategories() }
        val category =
            categories.firstOrNull { it.type == TransactionType.EXPENSE && it.id == "c_food" }
                ?: categories.firstOrNull { it.type == TransactionType.EXPENSE }
                ?: return
        val splitTransaction =
            Transaction(
                id = transactionId,
                userId = uid,
                amount = amount,
                type = TransactionType.EXPENSE,
                categoryId = category.id,
                category = category.name,
                note = "Hóa đơn chia tiền: ${bill.name}",
                date = bill.date,
                createdAt = System.currentTimeMillis(),
                source = TransactionSource.SPLIT,
                sourceGroupId = bill.groupId,
                sourceBillId = bill.id,
            )

        repository.updateTransaction(splitTransaction)
        if (legacyTransactionId != transactionId) {
            repository.deleteTransaction(legacyTransactionId)
        }
        if (emitNotification) {
            notificationRepository.insertNotification(
                NotificationFactory.fromPersonalTrigger(
                    PersonalNotificationTrigger(
                        relatedId = splitTransaction.id,
                        label = bill.name,
                        amount = amount,
                        categoryName = category.name,
                        triggerType = PersonalTriggerType.SPLIT_BRIDGED_TO_PERSONAL,
                    ),
                    uid,
                ),
            )
        }
    }

    private suspend fun syncSplitBillsForCurrentUser(uid: String) {
        if (uid.isBlank()) return

        val activeSplitTransactionIds = mutableSetOf<String>()
        val groups = splitRepository.getGroups().first()

        groups.forEach { group ->
            val bills = splitRepository.getBills(group.id).first()
            bills.forEach { bill ->
                val transactionId = splitTransactionId(bill.groupId, bill.id)
                if ((bill.shares[uid] ?: 0.0) > 0.0) {
                    activeSplitTransactionIds += transactionId
                }
                syncSplitBillTransactionForCurrentUser(
                    bill = bill,
                    uid = uid,
                    emitNotification = false,
                )
            }
        }

        repository.getAllTransactions()
            .filter { transaction -> transaction.source == TransactionSource.SPLIT }
            .filter { transaction -> transaction.id !in activeSplitTransactionIds }
            .forEach { transaction -> repository.deleteTransaction(transaction.id) }
    }

    private fun splitTransactionId(
        groupId: String,
        billId: String,
    ): String = "split_${groupId}_$billId"

    fun addCategory(
        name: String,
        description: String,
        type: TransactionType,
        isCustom: Boolean,
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
                        isActive = false,
                    ),
                )

                // Kích hoạt thông báo cho danh mục được tạo
                val notification =
                    NotificationFactory.categoryCreated(
                        categoryName = name,
                        type = type,
                        userId = currentUserId(),
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
        isActive: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val existing = _categories.value.firstOrNull { it.id == categoryId } ?: return@runCatching

                // Kiểm tra nếu loại thay đổi (CHI TIÊU <-> THU NHẬP)
                val typeChanged = existing.type != type

                // Cập nhật danh mục
                repository.updateCategory(
                    existing.copy(
                        name = name.trim(),
                        icon = iconCodeForName(name),
                        type = type,
                        isCustom = isCustom,
                        description = description.trim(),
                        isActive = isActive,
                    ),
                )

                // Nếu loại thay đổi, cập nhật tất cả giao dịch với danh mục này
                if (typeChanged) {
                    val allTransactions = repository.getAllTransactions()
                    val transactionsToUpdate = allTransactions.filter { it.categoryId == categoryId }
                    transactionsToUpdate.forEach { transaction ->
                        repository.updateTransaction(
                            transaction.copy(type = type),
                        )
                    }
                }

                // Luôn làm mới hoàn toàn sau khi cập nhật danh mục (dù loại có thay đổi hay không)
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

    fun refreshState() {
        val userId = currentUserId()
        if (userId.isBlank()) {
            refreshJob?.cancel()
            lastLoadedUserId = null
            clearAllData()
            return
        }

        lastLoadedUserId = userId
        refreshJob?.cancel()
        refreshJob =
            viewModelScope.launch(Dispatchers.IO) {
                refreshStateInternal(expectedUserId = userId)
            }
    }

    fun addSpendingReminder(
        categoryId: String?,
        categoryName: String,
        budgetAmount: Double,
        threshold: Float,
        reminderType: ReminderType,
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
                        reminderType = reminderType,
                    ),
                )

                // Kích hoạt thông báo cho lời nhắc được tạo
                val notification =
                    NotificationFactory.reminderCreated(
                        categoryName = categoryName,
                        budgetAmount = budgetAmount,
                        userId = currentUserId(),
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
        dayOfMonth: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val now = System.currentTimeMillis()
                val uid = currentUserId()
                val rule =
                    RecurringRule(
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
                        updatedAt = now,
                    )
                repository.insertRecurringRule(rule)
                notificationRepository.insertNotification(
                    NotificationFactory.fromPersonalTrigger(
                        PersonalNotificationTrigger(
                            relatedId = rule.id,
                            label = rule.name,
                            amount = rule.amount,
                            categoryName = rule.categoryName,
                            triggerType = PersonalTriggerType.RECURRING_RULE_CREATED,
                        ),
                        uid,
                    ),
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
        categoryId: String?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val now = System.currentTimeMillis()
                val uid = currentUserId()
                val normalizedCurrentAmount = currentAmount.coerceAtLeast(0.0)
                val goal =
                    PersonalGoal(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        title = title.trim(),
                        targetAmount = targetAmount,
                        currentAmount = normalizedCurrentAmount,
                        categoryId = categoryId?.takeIf { it.isNotBlank() },
                        deadlineAt = endOfCurrentMonth(),
                        status =
                            if (normalizedCurrentAmount >= targetAmount) {
                                GoalStatus.COMPLETED
                            } else {
                                GoalStatus.ACTIVE
                            },
                        createdAt = now,
                        updatedAt = now,
                    )
                repository.insertGoal(goal)
                notificationRepository.insertNotification(
                    NotificationFactory.fromPersonalTrigger(
                        PersonalNotificationTrigger(
                            relatedId = goal.id,
                            label = goal.title,
                            amount = goal.targetAmount,
                            triggerType = PersonalTriggerType.GOAL_CREATED,
                        ),
                        uid,
                    ),
                )
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    fun updateGoal(
        goalId: String,
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        categoryId: String?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                require(goalId.isNotBlank()) { "Không tìm thấy mục tiêu cần cập nhật." }
                require(title.isNotBlank()) { "Nhập tiêu đề mục tiêu." }
                require(targetAmount > 0.0) { "Số tiền mục tiêu phải lớn hơn 0." }

                val existingGoal =
                    _goals.value.firstOrNull { it.id == goalId }
                        ?: repository.getGoals().firstOrNull { it.id == goalId }
                        ?: error("Không tìm thấy mục tiêu cần cập nhật.")
                val normalizedCurrentAmount = currentAmount.coerceAtLeast(0.0)
                val updatedStatus =
                    when {
                        normalizedCurrentAmount >= targetAmount -> GoalStatus.COMPLETED
                        existingGoal.status == GoalStatus.PAUSED -> GoalStatus.PAUSED
                        else -> GoalStatus.ACTIVE
                    }

                repository.updateGoal(
                    existingGoal.copy(
                        userId = currentUserId(),
                        title = title.trim(),
                        targetAmount = targetAmount,
                        currentAmount = normalizedCurrentAmount,
                        categoryId = categoryId?.takeIf { it.isNotBlank() },
                        status = updatedStatus,
                        updatedAt = System.currentTimeMillis(),
                    ),
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
        balance: Double,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val now = System.currentTimeMillis()
                val uid = currentUserId()
                val wallet =
                    PersonalWallet(
                        id = UUID.randomUUID().toString(),
                        userId = uid,
                        name = name.trim(),
                        type = type,
                        balance = balance,
                        color = "#AB2D00",
                        isArchived = false,
                        createdAt = now,
                        updatedAt = now,
                    )
                repository.insertWallet(wallet)
                notificationRepository.insertNotification(
                    NotificationFactory.fromPersonalTrigger(
                        PersonalNotificationTrigger(
                            relatedId = wallet.id,
                            label = wallet.name,
                            amount = wallet.balance,
                            triggerType = PersonalTriggerType.WALLET_CREATED,
                        ),
                        uid,
                    ),
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

    private suspend fun refreshStateInternal(
        showLoading: Boolean = true,
        expectedUserId: String = currentUserId(),
    ) {
        if (expectedUserId.isBlank()) {
            clearAllData()
            return
        }
        if (!isCurrentUser(expectedUserId)) return

        if (showLoading) {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    currentUserId = expectedUserId,
                )
        }

        runCatching {
            lastLoadedUserId = expectedUserId
            val categories = repository.getCategories()
            if (!isCurrentUser(expectedUserId)) return

            var recurringRules = repository.getRecurringRules()
            if (!isCurrentUser(expectedUserId)) return

            var goals = repository.getGoals()
            if (!isCurrentUser(expectedUserId)) return

            val wallets = repository.getWallets()
            if (!isCurrentUser(expectedUserId)) return

            _categories.value = categories

            runCatching {
                syncSplitBillsForCurrentUser(expectedUserId)
            }
            if (!isCurrentUser(expectedUserId)) return

            var allTransactions = repository.getAllTransactions()
            if (!isCurrentUser(expectedUserId)) return

            val recurringSynced =
                syncDueRecurringTransactions(
                    recurringRules = recurringRules,
                    allTransactions = allTransactions,
                    expectedUserId = expectedUserId,
                )
            if (!isCurrentUser(expectedUserId)) return

            if (recurringSynced) {
                recurringRules = repository.getRecurringRules()
                if (!isCurrentUser(expectedUserId)) return

                allTransactions = repository.getAllTransactions()
            }
            if (!isCurrentUser(expectedUserId)) return

            goals =
                syncLinkedGoalsWithTransactions(
                    goals = goals,
                    transactions = allTransactions,
                    categories = categories,
                    expectedUserId = expectedUserId,
                )
            if (!isCurrentUser(expectedUserId)) return

            _allTransactions.value = allTransactions

            // Tối ưu hóa bộ nhớ: màn chính chỉ tải giao dịch tháng hiện tại trước.
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentYear = calendar.get(Calendar.YEAR)
            val filteredTransactions =
                allTransactions.filter { transaction ->
                    val txnCalendar =
                        Calendar.getInstance().apply {
                            timeInMillis = transaction.date
                        }
                    txnCalendar.get(Calendar.MONTH) + 1 == currentMonth &&
                        txnCalendar.get(Calendar.YEAR) == currentYear
                }
                    .take(500)
            val referenceMillis = System.currentTimeMillis()
            val upcomingRecurringExpense =
                recurringRules.toUpcomingRecurringExpense(referenceMillis = referenceMillis)
            val categoryTypesById = categories.associate { it.id to it.type }
            val savingsGoal =
                goals
                    .toPlanReserve(
                        categoryTypesById = categoryTypesById,
                        recurringRules = recurringRules,
                        referenceMillis = referenceMillis,
                    )

            _transactions.value = filteredTransactions
            _categories.value = categories
            _recurringRules.value = recurringRules
            _goals.value = goals
            _wallets.value = wallets

            _chartState.value =
                PersonalChartState(
                    pieSlices = filteredTransactions.toPieCategorySlices(),
                    dailyExpenseBars = filteredTransactions.toDailyExpenseBars(),
                    monthlySummary = filteredTransactions.toMonthlySummary(),
                    insights = allTransactions.toMonthlyInsights(),
                    safeToSpend =
                        allTransactions.toSafeToSpendForecast(
                            referenceMillis = referenceMillis,
                            upcomingRecurringExpense = upcomingRecurringExpense,
                            savingsGoal = savingsGoal,
                        ),
                )

            _uiState.value =
                buildUiState(
                    transactions = filteredTransactions,
                    categories = categories,
                    recurringRules = recurringRules,
                    goals = goals,
                    wallets = wallets,
                )

            // Tải tất cả giao dịch không đồng bộ cho lời nhắc (nền)
            viewModelScope.launch(Dispatchers.IO) {
                syncSpendingReminders(allTransactions, expectedUserId)
            }
        }.onFailure { throwable ->
            if (!isCurrentUser(expectedUserId)) return

            _transactions.value = emptyList()
            _allTransactions.value = emptyList()
            _categories.value = emptyList()
            _recurringRules.value = emptyList()
            _goals.value = emptyList()
            _wallets.value = emptyList()
            _chartState.value = PersonalChartState()
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    currentUserId = expectedUserId,
                    errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                )
        }
    }

    private fun buildUiState(
        transactions: List<Transaction>,
        categories: List<StoredCategory>,
        recurringRules: List<RecurringRule> = _recurringRules.value,
        goals: List<PersonalGoal> = _goals.value,
        wallets: List<PersonalWallet> = _wallets.value,
    ): PersonalUiState {
        val income =
            transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
        val expense =
            transactions
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
            balance = income - expense,
        )
    }

    private suspend fun syncDueRecurringTransactions(
        recurringRules: List<RecurringRule>,
        allTransactions: List<Transaction>,
        expectedUserId: String,
    ): Boolean {
        if (!isCurrentUser(expectedUserId)) return false

        val now = System.currentTimeMillis()
        val existingTransactionIds = allTransactions.map { it.id }.toMutableSet()
        val existingRecurringRuns =
            allTransactions
                .mapNotNull { transaction ->
                    transaction.recurringRuleId?.let { ruleId -> "$ruleId:${transaction.date}" }
                }
                .toMutableSet()
        var changed = false

        recurringRules
            .filter { rule -> rule.isEnabled }
            .forEach { rule ->
                var scheduledAt =
                    if (rule.nextRunAt > 0L) {
                        rule.nextRunAt
                    } else {
                        nextMonthlyRunAt(rule.dayOfMonth)
                    }
                var generatedRuns = 0

                while (scheduledAt <= now && generatedRuns < MAX_RECURRING_CATCH_UP_RUNS) {
                    val transactionId = recurringTransactionId(rule.id, scheduledAt)
                    val runKey = "${rule.id}:$scheduledAt"
                    if (transactionId !in existingTransactionIds && runKey !in existingRecurringRuns) {
                        repository.insertTransaction(
                            Transaction(
                                id = transactionId,
                                userId = expectedUserId,
                                amount = rule.amount,
                                type = rule.type,
                                categoryId = rule.categoryId,
                                category = rule.categoryName,
                                note = "Tự động từ ${rule.name}",
                                date = scheduledAt,
                                createdAt = now,
                                source = TransactionSource.RECURRING,
                                recurringRuleId = rule.id,
                            ),
                        )
                        existingTransactionIds += transactionId
                        existingRecurringRuns += runKey
                        changed = true
                    }

                    scheduledAt = nextRecurringRunAt(rule, scheduledAt)
                    generatedRuns++
                }

                if (scheduledAt != rule.nextRunAt) {
                    repository.updateRecurringRule(
                        rule.copy(
                            nextRunAt = scheduledAt,
                            updatedAt = now,
                        ),
                    )
                    changed = true
                }
            }

        return changed
    }

    private suspend fun syncLinkedGoalsWithTransactions(
        goals: List<PersonalGoal>,
        transactions: List<Transaction>,
        categories: List<StoredCategory>,
        expectedUserId: String,
    ): List<PersonalGoal> {
        if (!isCurrentUser(expectedUserId)) return goals

        val categoryTypesById = categories.associate { it.id to it.type }
        val transactionsByCategory = transactions.groupBy { it.categoryId }
        val now = System.currentTimeMillis()

        return goals.map { goal ->
            val categoryId = goal.categoryId?.takeIf { it.isNotBlank() } ?: return@map goal
            val referenceDate =
                when {
                    goal.deadlineAt > 0L -> goal.deadlineAt
                    goal.createdAt > 0L -> goal.createdAt
                    else -> now
                }
            val monthStart = startOfMonth(referenceDate)
            val monthEnd = endOfMonth(referenceDate)
            val syncedCurrentAmount =
                transactionsByCategory[categoryId]
                    .orEmpty()
                    .filter { transaction -> transaction.date in monthStart..monthEnd }
                    .sumOf { transaction -> transaction.amount }
                    .coerceAtLeast(0.0)
            val categoryType = categoryTypesById[categoryId]
            val syncedStatus =
                when {
                    goal.status == GoalStatus.PAUSED -> GoalStatus.PAUSED
                    categoryType == TransactionType.EXPENSE -> GoalStatus.ACTIVE
                    syncedCurrentAmount >= goal.targetAmount -> GoalStatus.COMPLETED
                    else -> GoalStatus.ACTIVE
                }
            val needsUpdate =
                kotlin.math.abs(goal.currentAmount - syncedCurrentAmount) >= 0.01 ||
                    goal.status != syncedStatus

            if (!needsUpdate) {
                goal
            } else {
                goal.copy(
                    userId = expectedUserId,
                    currentAmount = syncedCurrentAmount,
                    status = syncedStatus,
                    updatedAt = now,
                ).also { updatedGoal ->
                    repository.updateGoal(updatedGoal)
                }
            }
        }
    }

    private suspend fun syncSpendingReminders(
        allTransactions: List<Transaction>,
        expectedUserId: String = currentUserId(),
    ) {
        if (expectedUserId.isEmpty()) {
            _reminders.value = emptyList()
            return
        }
        if (!isCurrentUser(expectedUserId)) return

        val reminders = repository.getSpendingReminders()
        if (!isCurrentUser(expectedUserId)) return

        val activeReminders = reminders.filter { it.isEnabled }
        if (activeReminders.isEmpty()) {
            _reminders.value = reminders
            return
        }

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        activeReminders.forEach { reminder ->
            val reminderTransactions =
                allTransactions
                    .filter { transaction ->
                        transaction.type == TransactionType.EXPENSE &&
                            transaction.isInsideReminderWindow(
                                reminder = reminder,
                                referenceMillis = now,
                            )
                    }
            val spent =
                if (reminder.categoryId == null) {
                    reminderTransactions.sumOf { it.amount }
                } else {
                    reminderTransactions
                        .filter { transaction -> transaction.categoryId == reminder.categoryId }
                        .sumOf { transaction -> transaction.amount }
                }
            val effectiveThreshold =
                if (reminder.reminderType == ReminderType.MILESTONE) {
                    1f
                } else {
                    reminder.threshold
                }
            val isOverThreshold = spent >= reminder.budgetAmount * effectiveThreshold
            val wasNotAlertedToday =
                reminder.lastAlertedAt?.let { lastAlertedAt ->
                    val lastCalendar =
                        Calendar.getInstance().apply {
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
                                thresholdPercent = effectiveThreshold,
                            ),
                            expectedUserId,
                        ),
                    )
                    repository.updateSpendingReminder(
                        reminder.copy(
                            currentSpent = spent,
                            lastAlertedAt = System.currentTimeMillis(),
                        ),
                    )
                }
                spent != reminder.currentSpent -> {
                    repository.updateSpendingReminder(reminder.copy(currentSpent = spent))
                }
            }
        }

        val updatedReminders = repository.getSpendingReminders()
        if (isCurrentUser(expectedUserId)) {
            _reminders.value = updatedReminders
        }
    }

    private fun Transaction.isInsideReminderWindow(
        reminder: SpendingReminder,
        referenceMillis: Long,
    ): Boolean {
        val transactionCalendar = Calendar.getInstance().apply { timeInMillis = date }
        val referenceCalendar = Calendar.getInstance().apply { timeInMillis = referenceMillis }

        return when (reminder.reminderType) {
            ReminderType.DAILY ->
                transactionCalendar.get(Calendar.YEAR) == referenceCalendar.get(Calendar.YEAR) &&
                    transactionCalendar.get(Calendar.DAY_OF_YEAR) == referenceCalendar.get(Calendar.DAY_OF_YEAR)
            ReminderType.WEEKLY ->
                transactionCalendar.get(Calendar.YEAR) == referenceCalendar.get(Calendar.YEAR) &&
                    transactionCalendar.get(Calendar.WEEK_OF_YEAR) == referenceCalendar.get(Calendar.WEEK_OF_YEAR)
            ReminderType.MONTHLY ->
                transactionCalendar.get(Calendar.YEAR) == referenceCalendar.get(Calendar.YEAR) &&
                    transactionCalendar.get(Calendar.MONTH) == referenceCalendar.get(Calendar.MONTH)
            ReminderType.MILESTONE -> date >= reminder.createdAt
        }
    }

    private suspend fun Transaction.withUploadedReceiptIfNeeded(): Transaction {
        val receiptValue = receiptImageUrl?.takeIf { it.isNotBlank() } ?: return this
        val receiptUri = receiptValue.toUri()
        val isLocalReceipt = receiptUri.scheme == "content" || receiptUri.scheme == "file"
        if (!isLocalReceipt) return this

        val uploadedUrl = repository.uploadReceiptImage(id, receiptUri).getOrThrow()
        return copy(
            receiptImageUrl = uploadedUrl,
            source = if (source == TransactionSource.MANUAL) TransactionSource.RECEIPT else source,
        )
    }

    private fun setError(throwable: Throwable) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                isSaving = false,
                errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
            )
    }

    private fun currentUserId(): String {
        return FirebaseProviders.auth.currentUser?.uid.orEmpty()
    }

    private fun isCurrentUser(expectedUserId: String): Boolean {
        return expectedUserId.isNotBlank() && currentUserId() == expectedUserId
    }

    private fun iconCodeForName(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "OT"
            parts.size == 1 -> parts.first().take(2).uppercase()
            else -> "${parts[0].first()}${parts[1].first()}".uppercase()
        }
    }

    private fun startOfMonth(referenceMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun endOfMonth(referenceMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private fun nextMonthlyRunAt(dayOfMonth: Int): Long {
        val nowCalendar = Calendar.getInstance()
        val calendar = Calendar.getInstance()
        val targetDay = dayOfMonth.coerceIn(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.DAY_OF_MONTH, targetDay)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val targetIsBeforeToday =
            calendar.get(Calendar.YEAR) < nowCalendar.get(Calendar.YEAR) ||
                (
                    calendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) < nowCalendar.get(Calendar.DAY_OF_YEAR)
                )
        if (targetIsBeforeToday) {
            calendar.add(Calendar.MONTH, 1)
            calendar.set(
                Calendar.DAY_OF_MONTH,
                dayOfMonth.coerceIn(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)),
            )
        }
        return calendar.timeInMillis
    }

    private fun nextRecurringRunAt(
        rule: RecurringRule,
        previousRunAt: Long,
    ): Long {
        return when (rule.cadence) {
            RecurringCadence.WEEKLY ->
                Calendar.getInstance().apply {
                    timeInMillis = previousRunAt
                    add(Calendar.WEEK_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            RecurringCadence.MONTHLY ->
                Calendar.getInstance().apply {
                    timeInMillis = previousRunAt
                    add(Calendar.MONTH, 1)
                    set(
                        Calendar.DAY_OF_MONTH,
                        rule.dayOfMonth.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)),
                    )
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
        }
    }

    private fun recurringTransactionId(
        ruleId: String,
        scheduledAt: Long,
    ): String = "recurring_${ruleId}_$scheduledAt"

    private fun endOfCurrentMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private companion object {
        const val MAX_RECURRING_CATCH_UP_RUNS = 24
    }
}
