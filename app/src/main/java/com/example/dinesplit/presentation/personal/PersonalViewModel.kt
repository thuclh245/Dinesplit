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
import com.example.dinesplit.domain.model.PersonalReminderTrigger
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
    private var splitSyncJob: Job? = null

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
            splitSyncJob?.cancel()
            lastLoadedUserId = null
            clearAllData()
            return
        }

        if (userId != lastLoadedUserId || _uiState.value.currentUserId != userId) {
            refreshJob?.cancel()
            splitSyncJob?.cancel()
            lastLoadedUserId = userId
            clearAllData(currentUserId = userId, isLoading = true)
            startSplitBillSync(userId)
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
        splitSyncJob?.cancel()
    }

    /**
     * Thêm một giao dịch thủ công mới. Nếu giao dịch đi kèm hình ảnh hóa đơn cục bộ,
     * tự động tải ảnh lên Firebase Storage trước. Sau đó trừ/cộng số dư của ví liên kết (nếu có).
     *
     * @param transaction Đối tượng giao dịch [Transaction] cần thêm.
     */
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

                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
            }
        }
    }

    /**
     * Thêm một giao dịch phát sinh từ hóa đơn chia tiền trong nhóm.
     *
     * @param bill Đối tượng hóa đơn nhóm [Bill].
     */
    fun addSplitBillTransaction(bill: Bill) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val uid = currentUserId()
                if (uid.isBlank()) return@runCatching
                syncSplitBillTransactionForCurrentUser(bill = bill, uid = uid)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
            }
        }
    }

    /**
     * Xóa giao dịch chia tiền thuộc nhóm khỏi dữ liệu chi tiêu cá nhân.
     *
     * @param groupId ID của nhóm chia tiền.
     * @param billId ID của hóa đơn.
     */
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

    /**
     * Đối soát và cập nhật lại giao dịch chia tiền từ hóa đơn nhóm.
     *
     * @param bill Đối tượng hóa đơn nhóm [Bill].
     */
    fun reconcileSplitBillTransaction(bill: Bill) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val uid = currentUserId()
                if (uid.isBlank()) return@runCatching
                syncSplitBillTransactionForCurrentUser(bill = bill, uid = uid)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
            }
        }
    }

    /**
     * Đồng bộ hóa thông tin một hóa đơn chia tiền của nhóm thành giao dịch chi tiêu cá nhân của người dùng hiện tại.
     * Nếu số tiền chia nhỏ hơn hoặc bằng 0, hoặc hóa đơn không thuộc diện chi tiêu cá nhân của người dùng, giao dịch chia tiền sẽ bị xóa.
     *
     * @param bill Đối tượng hóa đơn nhóm [Bill].
     * @param uid ID người dùng hiện tại.
     */
    private suspend fun syncSplitBillTransactionForCurrentUser(
        bill: Bill,
        uid: String,
    ) {
        val transactionId = splitTransactionId(bill.groupId, bill.id)
        val legacyTransactionId = "split_${bill.id}"
        val amount = bill.shares[uid] ?: 0.0
        if (amount <= 0.0 || !bill.shouldCountAsPersonalExpense(uid)) {
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
    }

    /**
     * Tải và đồng bộ hóa toàn bộ các hóa đơn nhóm thành giao dịch cá nhân một lần duy nhất cho người dùng.
     *
     * @param uid ID người dùng hiện tại.
     */
    private suspend fun syncSplitBillsForCurrentUser(uid: String) {
        if (uid.isBlank()) return

        val groups = splitRepository.getGroups().first()
        val bills =
            groups.flatMap { group ->
                splitRepository.getBills(group.id).first()
            }

        syncSplitBillTransactionsForCurrentUser(uid = uid, bills = bills)
    }

    /**
     * Đồng bộ hóa hàng loạt danh sách hóa đơn nhóm thành giao dịch cá nhân.
     * Đồng thời tự động dọn dẹp các giao dịch chia tiền cũ không còn tồn tại hoặc không còn hiệu lực.
     *
     * @param uid ID người dùng hiện tại.
     * @param bills Danh sách các hóa đơn nhóm cần đồng bộ.
     */
    private suspend fun syncSplitBillTransactionsForCurrentUser(
        uid: String,
        bills: List<Bill>,
    ) {
        if (uid.isBlank()) return

        val activeSplitTransactionIds = mutableSetOf<String>()
        bills.forEach { bill ->
            val transactionId = splitTransactionId(bill.groupId, bill.id)
            if ((bill.shares[uid] ?: 0.0) > 0.0 && bill.shouldCountAsPersonalExpense(uid)) {
                activeSplitTransactionIds += transactionId
            }
            syncSplitBillTransactionForCurrentUser(
                bill = bill,
                uid = uid,
            )
        }

        repository.getAllTransactions()
            .filter { transaction -> transaction.source == TransactionSource.SPLIT }
            .filter { transaction -> transaction.id !in activeSplitTransactionIds }
            .forEach { transaction -> repository.deleteTransaction(transaction.id) }
    }

    /**
     * Khởi động cơ chế lắng nghe thời gian thực (reactive stream listener) từ cơ sở dữ liệu các nhóm chia tiền.
     * Bất kỳ khi nào có hóa đơn mới hoặc thay đổi chia tiền trong nhóm, hệ thống tự động chạy đồng bộ sang tài chính cá nhân.
     *
     * @param expectedUserId ID người dùng mong đợi để đồng bộ dữ liệu.
     */
    private fun startSplitBillSync(expectedUserId: String) {
        if (expectedUserId.isBlank()) return

        splitSyncJob =
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    splitRepository.getGroups().collectLatest { groups ->
                        if (!isCurrentUser(expectedUserId)) return@collectLatest

                        val activeGroups = groups.filter { group -> group.id.isNotBlank() }
                        if (activeGroups.isEmpty()) {
                            syncSplitBillTransactionsForCurrentUser(uid = expectedUserId, bills = emptyList())
                            refreshStateInternal(
                                showLoading = false,
                                expectedUserId = expectedUserId,
                                syncSplitBills = false,
                            )
                            return@collectLatest
                        }

                        val billFlows = activeGroups.map { group -> splitRepository.getBills(group.id) }
                        combine(billFlows) { billLists ->
                            billLists.flatMap { bills -> bills }
                        }.collectLatest { bills ->
                            if (!isCurrentUser(expectedUserId)) return@collectLatest
                            syncSplitBillTransactionsForCurrentUser(uid = expectedUserId, bills = bills)
                            refreshStateInternal(
                                showLoading = false,
                                expectedUserId = expectedUserId,
                                syncSplitBills = false,
                            )
                        }
                    }
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    if (isCurrentUser(expectedUserId)) {
                        setError(throwable)
                    }
                }
            }
    }

    /**
     * Tạo ID duy nhất cho giao dịch chia tiền thuộc nhóm dựa trên ID nhóm và ID hóa đơn.
     *
     * @param groupId ID của nhóm chia tiền.
     * @param billId ID của hóa đơn trong nhóm.
     * @return ID giao dịch duy nhất dạng chuỗi.
     */
    private fun splitTransactionId(
        groupId: String,
        billId: String,
    ): String = "split_${groupId}_$billId"

    /**
     * Hàm mở rộng kiểm tra xem hóa đơn chia tiền có được tính là chi phí cá nhân của người dùng hay không.
     * Hóa đơn được tính là chi phí nếu người dùng hiện tại là người thanh toán (payerId) hoặc nằm trong danh sách thành viên tham gia thanh toán.
     *
     * @param uid ID người dùng cần kiểm tra.
     * @return true nếu hóa đơn được tính là chi phí cá nhân, false nếu ngược lại.
     */
    private fun Bill.shouldCountAsPersonalExpense(uid: String): Boolean {
        return uid == payerId || uid in paidMemberIds
    }

    /**
     * Thêm một danh mục tài chính mới (Thu nhập hoặc Chi tiêu).
     *
     * @param name Tên danh mục.
     * @param description Mô tả danh mục.
     * @param type Loại danh mục [TransactionType].
     * @param isCustom true nếu danh mục do người dùng tự tạo, false nếu là mặc định.
     */
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

                refreshStateInternal(showLoading = false)
            }.onFailure { throwable ->
                setError(throwable)
            }
        }
    }

    /**
     * Cập nhật thông tin của danh mục tài chính đã tồn tại. Nếu loại danh mục thay đổi (ví dụ từ chi tiêu sang thu nhập),
     * tự động cập nhật lại loại của toàn bộ giao dịch đang tham chiếu đến danh mục này.
     *
     * @param categoryId ID của danh mục cần cập nhật.
     * @param name Tên danh mục mới.
     * @param description Mô tả mới.
     * @param type Loại danh mục mới.
     * @param isCustom Trạng thái tùy chỉnh mới.
     * @param isActive Trạng thái hoạt động mới.
     */
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

    /**
     * Xóa một danh mục tài chính.
     *
     * @param categoryId ID của danh mục cần xóa.
     */
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

    /**
     * Làm mới trạng thái tài chính của người dùng hiện tại một cách không đồng bộ.
     */
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

    /**
     * Thêm một nhắc nhở chi tiêu ngân sách mới. Đồng thời tự động tạo thông báo thông báo hệ thống về việc thiết lập nhắc nhở thành công.
     *
     * @param categoryId ID danh mục muốn đặt nhắc nhở (null nếu là nhắc nhở tổng ngân sách).
     * @param categoryName Tên danh mục muốn đặt nhắc nhở.
     * @param budgetAmount Hạn mức ngân sách tối đa.
     * @param threshold Ngưỡng cảnh báo chi tiêu (ví dụ 0.8 cho 80%).
     * @param reminderType Loại nhắc nhở [ReminderType] (Hằng ngày, hàng tuần, hàng tháng hoặc theo mốc).
     */
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

    /**
     * Thêm quy tắc giao dịch tự động lặp lại định kỳ mới.
     *
     * @param name Tên của quy tắc lặp lại.
     * @param amount Số tiền của giao dịch.
     * @param type Loại giao dịch [TransactionType] (Thu nhập hoặc Chi tiêu).
     * @param categoryId ID danh mục.
     * @param categoryName Tên danh mục.
     * @param cadence Chu kỳ lặp lại [RecurringCadence] (Tuần hoặc Tháng).
     * @param dayOfMonth Ngày cụ thể trong tháng để kích hoạt chạy.
     */
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
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    /**
     * Xóa một quy tắc giao dịch lặp lại định kỳ.
     *
     * @param ruleId ID của quy tắc cần xóa.
     */
    fun deleteRecurringRule(ruleId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.deleteRecurringRule(ruleId)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    /**
     * Thêm mục tiêu tiết kiệm mới cho người dùng.
     *
     * @param title Tiêu đề của mục tiêu.
     * @param targetAmount Số tiền mục tiêu cần tiết kiệm đạt tới.
     * @param currentAmount Số tiền tích lũy ban đầu có sẵn.
     * @param categoryId ID của danh mục thu nhập/tiết kiệm liên kết để đồng bộ tự động (nếu có).
     */
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
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    /**
     * Cập nhật thông tin của mục tiêu tiết kiệm đã có.
     *
     * @param goalId ID của mục tiêu cần sửa.
     * @param title Tiêu đề mục tiêu mới.
     * @param targetAmount Số tiền mục tiêu mới.
     * @param currentAmount Số tiền đã tích lũy hiện tại mới.
     * @param categoryId ID danh mục liên kết mới.
     */
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

    /**
     * Xóa một mục tiêu tiết kiệm.
     *
     * @param goalId ID của mục tiêu cần xóa.
     */
    fun deleteGoal(goalId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.deleteGoal(goalId)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    /**
     * Tạo một ví cá nhân mới chứa tiền.
     *
     * @param name Tên ví.
     * @param type Loại ví [WalletType] (Tiền mặt, Tài khoản ngân hàng, v.v.).
     * @param balance Số dư khởi tạo trong ví.
     */
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
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    /**
     * Xóa một ví cá nhân.
     *
     * @param walletId ID của ví cần xóa.
     */
    fun deleteWallet(walletId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.deleteWallet(walletId)
                refreshStateInternal(showLoading = false)
            }.onFailure { throwable -> setError(throwable) }
        }
    }

    /**
     * Xóa một thiết lập nhắc nhở chi tiêu ngân sách.
     *
     * @param reminderId ID của nhắc nhở cần xóa.
     */
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

    /**
     * Hàm nội bộ thực hiện tải lại toàn bộ trạng thái tài chính từ cơ sở dữ liệu.
     * Đồng bộ hóa hóa đơn nhóm (Split Bills), kiểm tra và tạo các giao dịch lặp lại định kỳ (Recurring),
     * đồng bộ mục tiêu tiết kiệm, tính toán các biểu đồ phân tích và kiểm tra các nhắc nhở ngân sách.
     *
     * @param showLoading Cho phép hiển thị màn hình đang tải (loading spinner) trên UI.
     * @param expectedUserId ID người dùng thực hiện cập nhật trạng thái.
     * @param syncSplitBills Cho phép đồng bộ hóa dữ liệu chia tiền từ nhóm.
     */
    private suspend fun refreshStateInternal(
        showLoading: Boolean = true,
        expectedUserId: String = currentUserId(),
        syncSplitBills: Boolean = true,
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

            if (syncSplitBills) {
                runCatching {
                    syncSplitBillsForCurrentUser(expectedUserId)
                }
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

    /**
     * Xây dựng đối tượng trạng thái giao diện [PersonalUiState] dựa trên danh sách giao dịch, danh mục,
     * quy tắc lặp lại, mục tiêu và danh sách ví hiện tại. Tính toán tổng thu, tổng chi và số dư.
     *
     * @param transactions Danh sách giao dịch cá nhân.
     * @param categories Danh sách các danh mục.
     * @param recurringRules Danh sách các quy tắc lặp lại định kỳ.
     * @param goals Danh sách các mục tiêu tiết kiệm.
     * @param wallets Danh sách các ví.
     * @return Đối tượng [PersonalUiState] đã tính toán đầy đủ số dư.
     */
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

    /**
     * Đồng bộ và sinh các giao dịch định kỳ đã đến hạn (catch-up runs).
     * Kiểm tra các quy tắc lặp lại xem có lịch chạy nào nhỏ hơn thời gian hiện tại không,
     * tự động tạo giao dịch cho mỗi chu kỳ bị trôi qua và cập nhật thời điểm chạy kế tiếp của quy tắc.
     *
     * @param recurringRules Danh sách các quy tắc lặp lại định kỳ.
     * @param allTransactions Toàn bộ danh sách giao dịch cá nhân.
     * @param expectedUserId ID người dùng kiểm tra đồng bộ.
     * @return true nếu có giao dịch định kỳ mới được tạo, false nếu ngược lại.
     */
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

    /**
     * Đồng bộ hóa tiến độ của mục tiêu tiết kiệm dựa trên tổng số giao dịch thuộc danh mục liên kết trong tháng hiện tại.
     * Cập nhật số tiền tiết kiệm hiện tại và chuyển trạng thái mục tiêu thành COMPLETED nếu đã tích lũy đủ.
     *
     * @param goals Danh sách mục tiêu tiết kiệm hiện có.
     * @param transactions Danh sách giao dịch cá nhân.
     * @param categories Danh sách danh mục.
     * @param expectedUserId ID người dùng thực hiện kiểm tra đồng bộ.
     * @return Danh sách mục tiêu tiết kiệm mới sau khi đã đồng bộ.
     */
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

    /**
     * Đồng bộ hóa số tiền đã chi tiêu thực tế với các thiết lập nhắc nhở chi tiêu ngân sách.
     * Nếu chi tiêu vượt quá giới hạn và chưa cảnh báo hôm nay, hệ thống tạo và lưu một thông báo cảnh báo.
     *
     * @param allTransactions Toàn bộ danh sách giao dịch cá nhân.
     * @param expectedUserId ID người dùng thực hiện kiểm tra đồng bộ.
     */
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

    /**
     * Hàm mở rộng kiểm tra xem giao dịch có nằm trong khung thời gian giới hạn của nhắc nhở hay không
     * (Daily: cùng ngày, Weekly: cùng tuần, Monthly: cùng tháng, Milestone: sau ngày tạo nhắc nhở).
     *
     * @param reminder Đối tượng nhắc nhở [SpendingReminder].
     * @param referenceMillis Thời điểm mốc so sánh (thường là thời gian hiện tại).
     * @return true nếu giao dịch nằm trong khung thời gian hợp lệ, false nếu ngược lại.
     */
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

    /**
     * Hàm mở rộng tải lên ảnh hóa đơn đính kèm nếu Uri của ảnh là cục bộ (Uri dạng content hoặc file).
     * Sau khi tải lên thành công, cập nhật URL hình ảnh từ xa và chuyển nguồn gốc giao dịch thành RECEIPT.
     *
     * @return Đối tượng [Transaction] mới chứa URL đã tải lên hoặc chính đối tượng ban đầu nếu không cần tải lên.
     */
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

    /**
     * Cập nhật trạng thái giao diện khi có lỗi xảy ra. Tắt trạng thái tải/lưu và đưa thông báo lỗi tới UI.
     *
     * @param throwable Lỗi xảy ra.
     */
    private fun setError(throwable: Throwable) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                isSaving = false,
                errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
            )
    }

    /**
     * Lấy ID người dùng hiện tại đang đăng nhập.
     *
     * @return ID người dùng (UID) hoặc chuỗi rỗng nếu chưa đăng nhập.
     */
    private fun currentUserId(): String {
        return FirebaseProviders.auth.currentUser?.uid.orEmpty()
    }

    /**
     * Kiểm tra xem ID người dùng truyền vào có khớp với ID người dùng hiện tại đang đăng nhập không.
     *
     * @param expectedUserId ID người dùng cần đối sánh.
     * @return true nếu trùng khớp và không rỗng, false nếu ngược lại.
     */
    private fun isCurrentUser(expectedUserId: String): Boolean {
        return expectedUserId.isNotBlank() && currentUserId() == expectedUserId
    }

    /**
     * Tự động sinh mã biểu tượng danh mục gồm 2 chữ cái viết hoa dựa trên tên danh mục.
     * Ví dụ: "Ăn uống" -> "AU", "Du lịch bụi" -> "DL".
     *
     * @param name Tên của danh mục.
     * @return Chuỗi gồm 2 chữ cái viết hoa.
     */
    private fun iconCodeForName(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "OT"
            parts.size == 1 -> parts.first().take(2).uppercase()
            else -> "${parts[0].first()}${parts[1].first()}".uppercase()
        }
    }

    /**
     * Lấy thời điểm bắt đầu của tháng chứa ngày truyền vào (00:00:00.000 ngày 1).
     *
     * @param referenceMillis Thời điểm mốc tính bằng mili-giây.
     * @return Thời điểm bắt đầu tháng tính bằng mili-giây.
     */
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

    /**
     * Lấy thời điểm kết thúc của tháng chứa ngày truyền vào (23:59:59.999 ngày cuối tháng).
     *
     * @param referenceMillis Thời điểm mốc tính bằng mili-giây.
     * @return Thời điểm cuối tháng tính bằng mili-giây.
     */
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

    /**
     * Tính toán thời điểm chạy tiếp theo của một quy tắc lặp lại hàng tháng dựa trên ngày trong tháng.
     * Nếu ngày chạy dự kiến đã qua so với hôm nay, ngày chạy sẽ tự động được dời sang tháng sau.
     *
     * @param dayOfMonth Ngày được chỉ định để chạy (1-31).
     * @return Thời điểm chạy tiếp theo tính bằng mili-giây.
     */
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

    /**
     * Tính toán thời điểm chạy kế tiếp của quy tắc định kỳ dựa trên thời điểm chạy trước đó và chu kỳ (Tuần/Tháng).
     *
     * @param rule Quy tắc lặp lại [RecurringRule].
     * @param previousRunAt Thời điểm chạy trước đó tính bằng mili-giây.
     * @return Thời điểm chạy kế tiếp tính bằng mili-giây.
     */
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

    /**
     * Tạo ID duy nhất cho một giao dịch tự động lặp lại định kỳ.
     *
     * @param ruleId ID của quy tắc lặp lại.
     * @param scheduledAt Thời gian lên lịch chạy giao dịch.
     * @return ID giao dịch duy nhất dạng chuỗi.
     */
    private fun recurringTransactionId(
        ruleId: String,
        scheduledAt: Long,
    ): String = "recurring_${ruleId}_$scheduledAt"

    /**
     * Lấy mốc thời gian kết thúc của tháng hiện tại (23:59:59.999 của ngày cuối cùng trong tháng này).
     *
     * @return Thời điểm cuối tháng tính bằng mili-giây.
     */
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
