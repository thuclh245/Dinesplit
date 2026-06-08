package com.example.dinesplit.data.repository

import android.content.Context
import android.net.Uri
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.GoalStatus
import com.example.dinesplit.domain.model.PersonalGoal
import com.example.dinesplit.domain.model.PersonalWallet
import com.example.dinesplit.domain.model.RecurringCadence
import com.example.dinesplit.domain.model.RecurringRule
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.SpendingReminder
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionSource
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.model.WalletType
import com.example.dinesplit.domain.repository.PersonalRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

/**
 * Lớp triển khai [PersonalRepository] sử dụng Firebase Firestore làm cơ sở dữ liệu lưu trữ
 * và Firebase Storage làm nơi lưu trữ tệp tin (ảnh hóa đơn).
 *
 * @property context Ngữ cảnh ứng dụng dùng để truy xuất thư viện cục bộ (nếu cần).
 * @property firestore Thực thể [FirebaseFirestore] để kết nối với cơ sở dữ liệu.
 */
class FirebasePersonalRepository private constructor(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val firestore: FirebaseFirestore = FirebaseProviders.firestore,
) : PersonalRepository {
    private val storage = FirebaseProviders.storage

    /**
     * Lấy toàn bộ danh sách giao dịch cá nhân của người dùng hiện tại từ Firestore.
     * Các giao dịch được sắp xếp theo thời gian ngày giao dịch giảm dần và lọc bỏ các giao dịch demo cũ.
     *
     * @return Danh sách [Transaction] của người dùng.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun getAllTransactions(): List<Transaction> {
        val uid = requireCurrentUserId()
        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_TRANSACTIONS)
                .orderBy(FIELD_DATE, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get(Source.SERVER)
                .awaitFirebase()

        return snapshot.documents
            .mapNotNull { document -> document.toTransaction(uid) }
            .filterNot { transaction -> transaction.isLegacyDemoSeedTransaction() }
    }

    /**
     * Lấy danh sách danh mục thu/chi cá nhân. Nếu tài khoản mới chưa có danh mục nào,
     * tự động khởi tạo danh sách danh mục mặc định.
     *
     * @return Danh sách [StoredCategory] đã sắp xếp theo loại (INCOME/EXPENSE) và tên.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun getCategories(): List<StoredCategory> {
        val uid = requireCurrentUserId()
        ensureDefaultCategories(uid)

        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_CATEGORIES)
                .get(Source.SERVER)
                .awaitFirebase()

        return snapshot.documents
            .mapNotNull { document -> document.toStoredCategory() }
            .sortedWith(compareBy<StoredCategory> { it.type.name }.thenBy { it.name.lowercase() })
    }

    /**
     * Thêm mới một giao dịch tài chính cá nhân vào Firestore.
     *
     * @param transaction Đối tượng [Transaction] cần thêm mới.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun insertTransaction(transaction: Transaction) {
        val uid = requireCurrentUserId()
        val normalizedTransaction = transaction.copy(userId = uid)
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_TRANSACTIONS)
            .document(normalizedTransaction.id)
            .set(normalizedTransaction.toFirestoreMap())
            .awaitFirebase()
    }

    /**
     * Cập nhật thông tin của một giao dịch tài chính cá nhân đã tồn tại.
     *
     * @param transaction Đối tượng [Transaction] chứa thông tin mới.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun updateTransaction(transaction: Transaction) {
        insertTransaction(transaction)
    }

    /**
     * Xóa một giao dịch tài chính cá nhân khỏi Firestore.
     *
     * @param transactionId ID của giao dịch cần xóa.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun deleteTransaction(transactionId: String) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_TRANSACTIONS)
            .document(transactionId)
            .delete()
            .awaitFirebase()
    }

    /**
     * Tải hình ảnh hóa đơn từ thiết bị lên Firebase Storage và liên kết URL tải xuống với ID giao dịch.
     *
     * @param transactionId ID của giao dịch liên kết với hóa đơn này.
     * @param receiptUri Uri cục bộ của hình ảnh hóa đơn trên thiết bị.
     * @return [Result] chứa URL hình ảnh sau khi tải lên thành công hoặc ngoại lệ nếu thất bại.
     */
    override suspend fun uploadReceiptImage(
        transactionId: String,
        receiptUri: Uri,
    ): Result<String> {
        return runCatching {
            val uid = requireCurrentUserId()
            val receiptReference = storage.reference.child("receipts/$uid/$transactionId.jpg")
            receiptReference.putFile(receiptUri).awaitFirebase()
            receiptReference.downloadUrl.awaitFirebase().toString()
        }
    }

    /**
     * Thêm mới một danh mục thu/chi tùy chỉnh vào Firestore.
     *
     * @param category Đối tượng [StoredCategory] cần lưu trữ.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun insertCategory(category: StoredCategory) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_CATEGORIES)
            .document(category.id)
            .set(category.toFirestoreMap())
            .awaitFirebase()
    }

    /**
     * Cập nhật thông tin của danh mục thu/chi đã tồn tại trong Firestore.
     *
     * @param category Đối tượng [StoredCategory] chứa dữ liệu mới.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun updateCategory(category: StoredCategory) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_CATEGORIES)
            .document(category.id)
            .set(category.toFirestoreMap())
            .awaitFirebase()
    }

    /**
     * Xóa một danh mục thu/chi cá nhân khỏi Firestore.
     *
     * @param categoryId ID của danh mục cần xóa.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun deleteCategory(categoryId: String) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_CATEGORIES)
            .document(categoryId)
            .delete()
            .awaitFirebase()
    }

    /**
     * Lấy danh sách các nhắc nhở/cảnh báo chi tiêu ngân sách của người dùng hiện tại từ Firestore.
     *
     * @return Danh sách [SpendingReminder] của người dùng.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun getSpendingReminders(): List<SpendingReminder> {
        val uid = requireCurrentUserId()
        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_REMINDERS)
                .get(Source.SERVER)
                .awaitFirebase()

        return snapshot.documents.mapNotNull { it.toSpendingReminder() }
    }

    /**
     * Thêm mới một nhắc nhở chi tiêu ngân sách vào Firestore.
     *
     * @param reminder Đối tượng [SpendingReminder] cần lưu trữ.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun insertSpendingReminder(reminder: SpendingReminder) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_REMINDERS)
            .document(reminder.id)
            .set(reminder.toFirestoreMap())
            .awaitFirebase()
    }

    /**
     * Cập nhật thông tin nhắc nhở chi tiêu ngân sách đã tồn tại.
     *
     * @param reminder Đối tượng [SpendingReminder] chứa thông tin cập nhật.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun updateSpendingReminder(reminder: SpendingReminder) {
        insertSpendingReminder(reminder)
    }

    /**
     * Xóa một nhắc nhở chi tiêu khỏi Firestore.
     *
     * @param reminderId ID của nhắc nhở cần xóa.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun deleteSpendingReminder(reminderId: String) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_REMINDERS)
            .document(reminderId)
            .delete()
            .awaitFirebase()
    }

    /**
     * Lấy danh sách các quy tắc lặp lại giao dịch định kỳ của người dùng từ Firestore.
     * Sắp xếp theo trạng thái kích hoạt và thời gian chạy tiếp theo.
     *
     * @return Danh sách [RecurringRule] của người dùng.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun getRecurringRules(): List<RecurringRule> {
        val uid = requireCurrentUserId()
        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_RECURRING_RULES)
                .get(Source.SERVER)
                .awaitFirebase()

        return snapshot.documents
            .mapNotNull { it.toRecurringRule(uid) }
            .sortedWith(compareBy<RecurringRule> { !it.isEnabled }.thenBy { it.nextRunAt })
    }

    /**
     * Thêm mới một quy tắc giao dịch định kỳ vào Firestore.
     *
     * @param rule Đối tượng [RecurringRule] cần thêm mới.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun insertRecurringRule(rule: RecurringRule) {
        val uid = requireCurrentUserId()
        val normalizedRule = rule.copy(userId = uid)
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_RECURRING_RULES)
            .document(normalizedRule.id)
            .set(normalizedRule.toFirestoreMap())
            .awaitFirebase()
    }

    /**
     * Cập nhật quy tắc giao dịch định kỳ đã tồn tại.
     *
     * @param rule Đối tượng [RecurringRule] chứa dữ liệu mới để cập nhật.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun updateRecurringRule(rule: RecurringRule) {
        insertRecurringRule(rule)
    }

    /**
     * Xóa một quy tắc giao dịch định kỳ khỏi Firestore.
     *
     * @param ruleId ID của quy tắc định kỳ cần xóa.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun deleteRecurringRule(ruleId: String) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_RECURRING_RULES)
            .document(ruleId)
            .delete()
            .awaitFirebase()
    }

    /**
     * Lấy danh sách các mục tiêu tiết kiệm cá nhân của người dùng từ Firestore.
     * Sắp xếp theo trạng thái và thời gian deadline.
     *
     * @return Danh sách [PersonalGoal] của người dùng.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun getGoals(): List<PersonalGoal> {
        val uid = requireCurrentUserId()
        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_GOALS)
                .get(Source.SERVER)
                .awaitFirebase()

        return snapshot.documents
            .mapNotNull { it.toPersonalGoal(uid) }
            .sortedWith(compareBy<PersonalGoal> { it.status.name }.thenBy { it.deadlineAt })
    }

    /**
     * Thêm mới một mục tiêu tiết kiệm cá nhân vào Firestore.
     *
     * @param goal Đối tượng [PersonalGoal] cần thêm mới.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun insertGoal(goal: PersonalGoal) {
        val uid = requireCurrentUserId()
        val normalizedGoal = goal.copy(userId = uid)
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_GOALS)
            .document(normalizedGoal.id)
            .set(normalizedGoal.toFirestoreMap())
            .awaitFirebase()
    }

    /**
     * Cập nhật thông tin/tiến độ của một mục tiêu tiết kiệm đã tồn tại.
     *
     * @param goal Đối tượng [PersonalGoal] cần cập nhật.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun updateGoal(goal: PersonalGoal) {
        insertGoal(goal)
    }

    /**
     * Xóa một mục tiêu tiết kiệm cá nhân khỏi Firestore.
     *
     * @param goalId ID của mục tiêu cần xóa.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun deleteGoal(goalId: String) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_GOALS)
            .document(goalId)
            .delete()
            .awaitFirebase()
    }

    /**
     * Lấy danh sách ví/tài khoản tài chính cá nhân từ Firestore.
     * Sắp xếp theo trạng thái lưu trữ (isArchived) và tên ví.
     *
     * @return Danh sách [PersonalWallet] của người dùng.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun getWallets(): List<PersonalWallet> {
        val uid = requireCurrentUserId()
        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_WALLETS)
                .get(Source.SERVER)
                .awaitFirebase()

        return snapshot.documents
            .mapNotNull { it.toPersonalWallet(uid) }
            .sortedWith(compareBy<PersonalWallet> { it.isArchived }.thenBy { it.name.lowercase() })
    }

    /**
     * Thêm mới một ví/tài khoản tài chính vào Firestore.
     *
     * @param wallet Đối tượng [PersonalWallet] cần thêm mới.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun insertWallet(wallet: PersonalWallet) {
        val uid = requireCurrentUserId()
        val normalizedWallet = wallet.copy(userId = uid)
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_WALLETS)
            .document(normalizedWallet.id)
            .set(normalizedWallet.toFirestoreMap())
            .awaitFirebase()
    }

    /**
     * Cập nhật thông tin số dư hoặc cấu hình ví tài chính đã tồn tại.
     *
     * @param wallet Đối tượng [PersonalWallet] chứa dữ liệu cập nhật.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun updateWallet(wallet: PersonalWallet) {
        insertWallet(wallet)
    }

    /**
     * Xóa một ví/tài khoản tài chính khỏi Firestore.
     *
     * @param walletId ID của ví cần xóa.
     * @throws IllegalStateException nếu người dùng chưa đăng nhập.
     */
    override suspend fun deleteWallet(walletId: String) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_PERSONAL)
            .document(uid)
            .collection(COLLECTION_WALLETS)
            .document(walletId)
            .delete()
            .awaitFirebase()
    }

    /**
     * Khởi tạo các danh mục mặc định cho tài khoản người dùng mới nếu danh mục trống.
     *
     * @param uid ID người dùng hiện tại cần tạo các danh mục mặc định.
     */
    private suspend fun ensureDefaultCategories(uid: String) {
        val categoriesRef =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_CATEGORIES)
        val existing = categoriesRef.limit(1).get().awaitFirebase()

        if (!existing.isEmpty) return

        val batch = firestore.batch()
        defaultCategories().forEach { category ->
            batch.set(categoriesRef.document(category.id), category.toFirestoreMap())
        }
        batch.commit().awaitFirebase()
    }

    /**
     * Lấy ID người dùng hiện tại từ Firebase Auth.
     *
     * @return ID người dùng (UID).
     * @throws IllegalStateException nếu chưa đăng nhập.
     */
    private fun requireCurrentUserId(): String {
        return FirebaseProviders.auth.currentUser?.uid
            ?: throw IllegalStateException("Vui lòng đăng nhập để sử dụng dữ liệu cá nhân")
    }

    /**
     * Trích xuất giá trị thời gian dạng Long từ Firestore Document Snapshot một cách an toàn.
     * Hỗ trợ chuyển đổi từ kiểu dữ liệu Timestamp hoặc số Long thuần túy.
     *
     * @param field Tên trường dữ liệu cần trích xuất.
     * @return Giá trị thời gian tính bằng mili-giây (Long) hoặc null nếu không tồn tại/lỗi.
     */
    private fun DocumentSnapshot.getLongDateSafe(field: String): Long? {
        return try {
            getTimestamp(field)?.toDate()?.time
        } catch (e: Exception) {
            try {
                getLong(field)
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Chuyển đổi dữ liệu từ DocumentSnapshot sang đối tượng [Transaction] an toàn.
     *
     * @param uid ID người dùng sở hữu giao dịch.
     * @return Đối tượng [Transaction] hoặc null nếu dữ liệu thiếu các trường bắt buộc.
     */
    private fun DocumentSnapshot.toTransaction(uid: String): Transaction? {
        val idValue = getString(FIELD_ID) ?: id
        val type =
            getString(FIELD_TYPE)?.let { value ->
                TransactionType.entries.firstOrNull { it.name == value }
            } ?: return null
        val source =
            getString(FIELD_SOURCE)?.let { value ->
                TransactionSource.entries.firstOrNull { it.name == value }
            } ?: TransactionSource.MANUAL

        return Transaction(
            id = idValue,
            userId = getString(FIELD_USER_ID) ?: uid,
            amount = getNumberDouble(FIELD_AMOUNT) ?: return null,
            type = type,
            categoryId = getString(FIELD_CATEGORY_ID) ?: return null,
            category = getString(FIELD_CATEGORY) ?: return null,
            note = getString(FIELD_NOTE)?.takeIf { it.isNotBlank() },
            date = getLongDateSafe(FIELD_DATE) ?: return null,
            createdAt = getLongDateSafe(FIELD_CREATED_AT) ?: 0L,
            source = source,
            sourceGroupId = getString(FIELD_SOURCE_GROUP_ID)?.takeIf { it.isNotBlank() },
            sourceBillId = getString(FIELD_SOURCE_BILL_ID)?.takeIf { it.isNotBlank() },
            recurringRuleId = getString(FIELD_RECURRING_RULE_ID)?.takeIf { it.isNotBlank() },
            receiptImageUrl = getString(FIELD_RECEIPT_IMAGE_URL)?.takeIf { it.isNotBlank() },
            walletId = getString(FIELD_WALLET_ID)?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Kiểm tra xem giao dịch có phải là giao dịch demo/mẫu cũ hay không.
     * Tránh hiển thị các dữ liệu thử nghiệm cũ trong giao diện thực tế của người dùng.
     *
     * @return true nếu đây là giao dịch mẫu cũ, false nếu ngược lại.
     */
    private fun Transaction.isLegacyDemoSeedTransaction(): Boolean {
        if (source != TransactionSource.MANUAL) return false
        if (!sourceGroupId.isNullOrBlank() || !sourceBillId.isNullOrBlank()) return false
        if (!recurringRuleId.isNullOrBlank() || !receiptImageUrl.isNullOrBlank() || !walletId.isNullOrBlank()) return false

        return legacyDemoTransactionSignatures.any { signature ->
            categoryId == signature.categoryId &&
                amount == signature.amount &&
                type == signature.type &&
                abs((createdAt - date) - signature.createdBeforeDateByMs) <= LEGACY_DEMO_TIME_TOLERANCE_MS
        }
    }

    /**
     * Chuyển đổi dữ liệu từ DocumentSnapshot sang đối tượng [StoredCategory].
     *
     * @return Đối tượng [StoredCategory] hoặc null nếu thiếu các trường bắt buộc.
     */
    private fun DocumentSnapshot.toStoredCategory(): StoredCategory? {
        val type =
            getString(FIELD_TYPE)?.let { value ->
                TransactionType.entries.firstOrNull { it.name == value }
            } ?: return null

        return StoredCategory(
            id = getString(FIELD_ID) ?: id,
            name = getString(FIELD_NAME) ?: return null,
            icon = getString(FIELD_ICON) ?: "",
            type = type,
            isCustom = getBoolean(FIELD_IS_CUSTOM) ?: false,
            description = getString(FIELD_DESCRIPTION) ?: "",
            amountLabel = getString(FIELD_AMOUNT_LABEL) ?: "0 VND",
            progress = getNumberDouble(FIELD_PROGRESS)?.toFloat() ?: 0f,
            isActive = getBoolean(FIELD_IS_ACTIVE) ?: false,
        )
    }

    /**
     * Chuyển đổi dữ liệu từ DocumentSnapshot sang đối tượng [SpendingReminder].
     *
     * @return Đối tượng [SpendingReminder] hoặc null nếu lỗi phân tích dữ liệu.
     */
    private fun DocumentSnapshot.toSpendingReminder(): SpendingReminder? {
        val type =
            getString(FIELD_REMINDER_TYPE)?.let { value ->
                ReminderType.entries.firstOrNull { it.name == value }
            } ?: ReminderType.MONTHLY

        return SpendingReminder(
            id = getString(FIELD_ID) ?: id,
            userId = getString(FIELD_USER_ID) ?: "",
            categoryId = getString(FIELD_CATEGORY_ID)?.takeIf { it.isNotBlank() },
            categoryName = getString(FIELD_CATEGORY_NAME) ?: "Overall",
            budgetAmount = getNumberDouble(FIELD_BUDGET_AMOUNT) ?: 0.0,
            currentSpent = getNumberDouble(FIELD_CURRENT_SPENT) ?: 0.0,
            threshold = getNumberDouble(FIELD_THRESHOLD)?.toFloat() ?: 0.8f,
            reminderType = type,
            isEnabled = getBoolean(FIELD_IS_ENABLED) ?: true,
            lastAlertedAt = getLongDateSafe(FIELD_LAST_ALERTED_AT),
            createdAt = getLongDateSafe(FIELD_CREATED_AT) ?: 0L,
            updatedAt = getLongDateSafe(FIELD_UPDATED_AT) ?: 0L,
        )
    }

    /**
     * Chuyển đổi dữ liệu từ DocumentSnapshot sang đối tượng [RecurringRule].
     *
     * @param uid ID người dùng sở hữu quy tắc định kỳ này.
     * @return Đối tượng [RecurringRule] hoặc null nếu lỗi phân tích dữ liệu.
     */
    private fun DocumentSnapshot.toRecurringRule(uid: String): RecurringRule? {
        val type =
            getString(FIELD_TYPE)?.let { value ->
                TransactionType.entries.firstOrNull { it.name == value }
            } ?: TransactionType.EXPENSE
        val cadence =
            getString(FIELD_CADENCE)?.let { value ->
                RecurringCadence.entries.firstOrNull { it.name == value }
            } ?: RecurringCadence.MONTHLY

        return RecurringRule(
            id = getString(FIELD_ID) ?: id,
            userId = getString(FIELD_USER_ID) ?: uid,
            name = getString(FIELD_NAME) ?: return null,
            amount = getNumberDouble(FIELD_AMOUNT) ?: return null,
            type = type,
            categoryId = getString(FIELD_CATEGORY_ID) ?: return null,
            categoryName = getString(FIELD_CATEGORY_NAME) ?: return null,
            cadence = cadence,
            dayOfMonth = getLong(FIELD_DAY_OF_MONTH)?.toInt()?.coerceIn(1, 31) ?: 1,
            nextRunAt = getLongDateSafe(FIELD_NEXT_RUN_AT) ?: 0L,
            isEnabled = getBoolean(FIELD_IS_ENABLED) ?: true,
            createdAt = getLongDateSafe(FIELD_CREATED_AT) ?: 0L,
            updatedAt = getLongDateSafe(FIELD_UPDATED_AT) ?: 0L,
        )
    }

    /**
     * Chuyển đổi dữ liệu từ DocumentSnapshot sang đối tượng [PersonalGoal] mục tiêu tiết kiệm.
     *
     * @param uid ID người dùng sở hữu mục tiêu.
     * @return Đối tượng [PersonalGoal] hoặc null nếu lỗi phân tích dữ liệu.
     */
    private fun DocumentSnapshot.toPersonalGoal(uid: String): PersonalGoal? {
        val status =
            getString(FIELD_STATUS)?.let { value ->
                GoalStatus.entries.firstOrNull { it.name == value }
            } ?: GoalStatus.ACTIVE

        return PersonalGoal(
            id = getString(FIELD_ID) ?: id,
            userId = getString(FIELD_USER_ID) ?: uid,
            title = getString(FIELD_TITLE) ?: return null,
            targetAmount = getNumberDouble(FIELD_TARGET_AMOUNT) ?: return null,
            currentAmount = getNumberDouble(FIELD_CURRENT_AMOUNT) ?: 0.0,
            categoryId = getString(FIELD_CATEGORY_ID)?.takeIf { it.isNotBlank() },
            deadlineAt = getLongDateSafe(FIELD_DEADLINE_AT) ?: 0L,
            status = status,
            createdAt = getLongDateSafe(FIELD_CREATED_AT) ?: 0L,
            updatedAt = getLongDateSafe(FIELD_UPDATED_AT) ?: 0L,
        )
    }

    /**
     * Chuyển đổi dữ liệu từ DocumentSnapshot sang đối tượng [PersonalWallet] ví tiền.
     *
     * @param uid ID người dùng sở hữu ví.
     * @return Đối tượng [PersonalWallet] hoặc null nếu lỗi phân tích dữ liệu.
     */
    private fun DocumentSnapshot.toPersonalWallet(uid: String): PersonalWallet? {
        val type =
            getString(FIELD_WALLET_TYPE)?.let { value ->
                WalletType.entries.firstOrNull { it.name == value }
            } ?: WalletType.CASH

        return PersonalWallet(
            id = getString(FIELD_ID) ?: id,
            userId = getString(FIELD_USER_ID) ?: uid,
            name = getString(FIELD_NAME) ?: return null,
            type = type,
            balance = getNumberDouble(FIELD_BALANCE) ?: 0.0,
            color = getString(FIELD_COLOR) ?: "#AB2D00",
            isArchived = getBoolean(FIELD_IS_ARCHIVED) ?: false,
            createdAt = getLongDateSafe(FIELD_CREATED_AT) ?: 0L,
            updatedAt = getLongDateSafe(FIELD_UPDATED_AT) ?: 0L,
        )
    }

    /**
     * Lấy giá trị số dạng Double từ một trường dữ liệu của Document.
     *
     * @param field Tên trường dữ liệu.
     * @return Giá trị Double hoặc null nếu không thể chuyển đổi.
     */
    private fun DocumentSnapshot.getNumberDouble(field: String): Double? {
        return (get(field) as? Number)?.toDouble()
    }

    /**
     * Chuyển đổi đối tượng [Transaction] thành Map dữ liệu phù hợp với định dạng lưu trữ của Firestore.
     *
     * @return Map dữ liệu dạng String to Any.
     */
    private fun Transaction.toFirestoreMap(): Map<String, Any> {
        val now = System.currentTimeMillis()
        return mapOf(
            FIELD_ID to id,
            FIELD_USER_ID to userId,
            FIELD_AMOUNT to amount,
            FIELD_TYPE to type.name,
            FIELD_CATEGORY_ID to categoryId,
            FIELD_CATEGORY to category,
            FIELD_NOTE to note.orEmpty(),
            FIELD_DATE to date,
            FIELD_CREATED_AT to createdAt,
            FIELD_UPDATED_AT to now,
            FIELD_SOURCE to source.name,
            FIELD_SOURCE_GROUP_ID to sourceGroupId.orEmpty(),
            FIELD_SOURCE_BILL_ID to sourceBillId.orEmpty(),
            FIELD_RECURRING_RULE_ID to recurringRuleId.orEmpty(),
            FIELD_RECEIPT_IMAGE_URL to receiptImageUrl.orEmpty(),
            FIELD_WALLET_ID to walletId.orEmpty(),
        )
    }

    /**
     * Chuyển đổi đối tượng [StoredCategory] thành Map dữ liệu phù hợp để đẩy lên Firestore.
     *
     * @return Map dữ liệu dạng String to Any.
     */
    private fun StoredCategory.toFirestoreMap(): Map<String, Any> {
        val now = System.currentTimeMillis()
        return mapOf(
            FIELD_ID to id,
            FIELD_NAME to name,
            FIELD_ICON to icon,
            FIELD_TYPE to type.name,
            FIELD_IS_CUSTOM to isCustom,
            FIELD_DESCRIPTION to description,
            FIELD_AMOUNT_LABEL to amountLabel,
            FIELD_PROGRESS to progress.toDouble(),
            FIELD_IS_ACTIVE to isActive,
            FIELD_CREATED_AT to now,
            FIELD_UPDATED_AT to now,
        )
    }

    /**
     * Chuyển đổi đối tượng [SpendingReminder] thành Map dữ liệu phù hợp để lưu trữ lên Firestore.
     *
     * @return Map dữ liệu dạng String to Any.
     */
    private fun SpendingReminder.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            FIELD_ID to id,
            FIELD_USER_ID to userId,
            FIELD_CATEGORY_ID to categoryId.orEmpty(),
            FIELD_CATEGORY_NAME to categoryName,
            FIELD_BUDGET_AMOUNT to budgetAmount,
            FIELD_CURRENT_SPENT to currentSpent,
            FIELD_THRESHOLD to threshold,
            FIELD_REMINDER_TYPE to reminderType.name,
            FIELD_IS_ENABLED to isEnabled,
            FIELD_LAST_ALERTED_AT to (lastAlertedAt ?: 0L),
            FIELD_CREATED_AT to createdAt,
            FIELD_UPDATED_AT to System.currentTimeMillis(),
        )
    }

    /**
     * Chuyển đổi đối tượng [RecurringRule] thành Map dữ liệu phù hợp để lưu trữ lên Firestore.
     *
     * @return Map dữ liệu dạng String to Any.
     */
    private fun RecurringRule.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            FIELD_ID to id,
            FIELD_USER_ID to userId,
            FIELD_NAME to name,
            FIELD_AMOUNT to amount,
            FIELD_TYPE to type.name,
            FIELD_CATEGORY_ID to categoryId,
            FIELD_CATEGORY_NAME to categoryName,
            FIELD_CADENCE to cadence.name,
            FIELD_DAY_OF_MONTH to dayOfMonth.coerceIn(1, 31),
            FIELD_NEXT_RUN_AT to nextRunAt,
            FIELD_IS_ENABLED to isEnabled,
            FIELD_CREATED_AT to createdAt,
            FIELD_UPDATED_AT to System.currentTimeMillis(),
        )
    }

    /**
     * Chuyển đổi đối tượng [PersonalGoal] thành Map dữ liệu để lưu trữ lên Firestore.
     *
     * @return Map dữ liệu dạng String to Any.
     */
    private fun PersonalGoal.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            FIELD_ID to id,
            FIELD_USER_ID to userId,
            FIELD_TITLE to title,
            FIELD_TARGET_AMOUNT to targetAmount,
            FIELD_CURRENT_AMOUNT to currentAmount,
            FIELD_CATEGORY_ID to categoryId.orEmpty(),
            FIELD_DEADLINE_AT to deadlineAt,
            FIELD_STATUS to status.name,
            FIELD_CREATED_AT to createdAt,
            FIELD_UPDATED_AT to System.currentTimeMillis(),
        )
    }

    /**
     * Chuyển đổi đối tượng [PersonalWallet] thành Map dữ liệu phù hợp để lưu trữ lên Firestore.
     *
     * @return Map dữ liệu dạng String to Any.
     */
    private fun PersonalWallet.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            FIELD_ID to id,
            FIELD_USER_ID to userId,
            FIELD_NAME to name,
            FIELD_WALLET_TYPE to type.name,
            FIELD_BALANCE to balance,
            FIELD_COLOR to color,
            FIELD_IS_ARCHIVED to isArchived,
            FIELD_CREATED_AT to createdAt,
            FIELD_UPDATED_AT to System.currentTimeMillis(),
        )
    }

    /**
     * Hàm mở rộng để chuyển đổi các tác vụ [Task] không đồng bộ của Firebase thành suspend function trong coroutines.
     *
     * @return Kết quả trả về của Task.
     * @throws Exception Ngoại lệ xảy ra trong quá trình thực thi tác vụ Firebase.
     */
    private suspend fun <T> Task<T>.awaitFirebase(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Tác vụ Firebase thất bại"),
                    )
                }
            }
        }
    }

    companion object {
        private const val COLLECTION_USER_PERSONAL = "user_personal"
        private const val COLLECTION_TRANSACTIONS = "transactions"
        private const val COLLECTION_CATEGORIES = "categories"
        private const val COLLECTION_REMINDERS = "reminders"
        private const val COLLECTION_RECURRING_RULES = "recurring_rules"
        private const val COLLECTION_GOALS = "goals"
        private const val COLLECTION_WALLETS = "wallets"

        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_AMOUNT = "amount"
        private const val FIELD_TYPE = "type"
        private const val FIELD_CATEGORY_ID = "categoryId"
        private const val FIELD_CATEGORY = "category"
        private const val FIELD_NOTE = "note"
        private const val FIELD_DATE = "date"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_NAME = "name"
        private const val FIELD_ICON = "icon"
        private const val FIELD_IS_CUSTOM = "isCustom"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_AMOUNT_LABEL = "amountLabel"
        private const val FIELD_PROGRESS = "progress"
        private const val FIELD_IS_ACTIVE = "isActive"
        private const val FIELD_CATEGORY_NAME = "categoryName"
        private const val FIELD_BUDGET_AMOUNT = "budgetAmount"
        private const val FIELD_CURRENT_SPENT = "currentSpent"
        private const val FIELD_THRESHOLD = "threshold"
        private const val FIELD_REMINDER_TYPE = "reminderType"
        private const val FIELD_IS_ENABLED = "isEnabled"
        private const val FIELD_LAST_ALERTED_AT = "lastAlertedAt"
        private const val FIELD_SOURCE = "source"
        private const val FIELD_SOURCE_GROUP_ID = "sourceGroupId"
        private const val FIELD_SOURCE_BILL_ID = "sourceBillId"
        private const val FIELD_RECURRING_RULE_ID = "recurringRuleId"
        private const val FIELD_RECEIPT_IMAGE_URL = "receiptImageUrl"
        private const val FIELD_WALLET_ID = "walletId"
        private const val FIELD_CADENCE = "cadence"
        private const val FIELD_DAY_OF_MONTH = "dayOfMonth"
        private const val FIELD_NEXT_RUN_AT = "nextRunAt"
        private const val FIELD_TITLE = "title"
        private const val FIELD_TARGET_AMOUNT = "targetAmount"
        private const val FIELD_CURRENT_AMOUNT = "currentAmount"
        private const val FIELD_DEADLINE_AT = "deadlineAt"
        private const val FIELD_STATUS = "status"
        private const val FIELD_WALLET_TYPE = "walletType"
        private const val FIELD_BALANCE = "balance"
        private const val FIELD_COLOR = "color"
        private const val FIELD_IS_ARCHIVED = "isArchived"
        private const val ONE_HOUR_MS = 60L * 60L * 1000L
        private const val ONE_DAY_MS = 24L * ONE_HOUR_MS
        private const val LEGACY_DEMO_TIME_TOLERANCE_MS = 1000L

        private val legacyDemoTransactionSignatures =
            listOf(
                LegacyDemoTransactionSignature("c_food", 150_000.0, TransactionType.EXPENSE, 2L * ONE_HOUR_MS),
                LegacyDemoTransactionSignature("c_transit", 50_000.0, TransactionType.EXPENSE, ONE_DAY_MS),
                LegacyDemoTransactionSignature("c_grocery", 320_000.0, TransactionType.EXPENSE, 2L * ONE_DAY_MS),
                LegacyDemoTransactionSignature("c_bonus", 200_000.0, TransactionType.INCOME, 3L * ONE_DAY_MS),
                LegacyDemoTransactionSignature("c_fun", 100_000.0, TransactionType.EXPENSE, 4L * ONE_DAY_MS),
            )

        @Volatile
        private var INSTANCE: FirebasePersonalRepository? = null

        /**
         * Lấy instance duy nhất (Singleton) của [FirebasePersonalRepository].
         *
         * @param context Ngữ cảnh ứng dụng.
         * @return Thực thể [FirebasePersonalRepository].
         */
        fun getInstance(context: Context): FirebasePersonalRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebasePersonalRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Danh sách các danh mục mặc định ban đầu được thiết lập cho tài khoản người dùng mới.
         *
         * @return Danh sách [StoredCategory] mặc định.
         */
        private fun defaultCategories(): List<StoredCategory> {
            return listOf(
                StoredCategory("c_food", "Dining Out", "FD", TransactionType.EXPENSE, false, "Restaurants, cafes, and delivery.", "0 VND", 0f, true),
                StoredCategory("c_grocery", "Groceries", "GR", TransactionType.EXPENSE, false, "Supermarkets and local markets.", "0 VND", 0f, false),
                StoredCategory("c_transit", "Transit", "TR", TransactionType.EXPENSE, false, "Rideshares and public transport.", "0 VND", 0f, false),
                StoredCategory("c_fun", "Entertainment", "EN", TransactionType.EXPENSE, true, "Movies, events, and subscriptions.", "0 VND", 0f, false),
                StoredCategory("c_salary", "Salary", "SL", TransactionType.INCOME, false, "Monthly fixed salary income.", "0 VND", 0f, true),
                StoredCategory("c_bonus", "Bonus", "BN", TransactionType.INCOME, false, "Project and performance rewards.", "0 VND", 0f, false),
                StoredCategory("c_gift", "Gift", "GF", TransactionType.INCOME, true, "Personal gifts and contributions.", "0 VND", 0f, false),
                StoredCategory("c_other_income", "Other", "OT", TransactionType.INCOME, true, "Other incoming cash flows.", "0 VND", 0f, false),
            )
        }
    }

    /**
     * Dữ liệu mô phỏng cấu trúc của một giao dịch mẫu cũ.
     */
    private data class LegacyDemoTransactionSignature(
        val categoryId: String,
        val amount: Double,
        val type: TransactionType,
        val createdBeforeDateByMs: Long,
    )
}
