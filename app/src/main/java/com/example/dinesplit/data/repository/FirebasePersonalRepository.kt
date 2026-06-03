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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebasePersonalRepository private constructor(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val firestore: FirebaseFirestore = FirebaseProviders.firestore,
) : PersonalRepository {
    private val storage = FirebaseProviders.storage

    override suspend fun getAllTransactions(): List<Transaction> {
        val uid = requireCurrentUserId()
        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_TRANSACTIONS)
                .orderBy(FIELD_DATE, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .awaitFirebase()

        return snapshot.documents.mapNotNull { document ->
            document.toTransaction(uid)
        }
    }

    override suspend fun getCategories(): List<StoredCategory> {
        val uid = requireCurrentUserId()
        ensureDefaultCategories(uid)

        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_CATEGORIES)
                .get()
                .awaitFirebase()

        return snapshot.documents
            .mapNotNull { document -> document.toStoredCategory() }
            .sortedWith(compareBy<StoredCategory> { it.type.name }.thenBy { it.name.lowercase() })
    }

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

    override suspend fun updateTransaction(transaction: Transaction) {
        insertTransaction(transaction)
    }

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

    override suspend fun getSpendingReminders(): List<SpendingReminder> {
        val uid = requireCurrentUserId()
        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_REMINDERS)
                .get()
                .awaitFirebase()

        return snapshot.documents.mapNotNull { it.toSpendingReminder() }
    }

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

    override suspend fun updateSpendingReminder(reminder: SpendingReminder) {
        insertSpendingReminder(reminder)
    }

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

    override suspend fun getRecurringRules(): List<RecurringRule> {
        val uid = requireCurrentUserId()
        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_RECURRING_RULES)
                .get()
                .awaitFirebase()

        return snapshot.documents
            .mapNotNull { it.toRecurringRule(uid) }
            .sortedWith(compareBy<RecurringRule> { !it.isEnabled }.thenBy { it.nextRunAt })
    }

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

    override suspend fun updateRecurringRule(rule: RecurringRule) {
        insertRecurringRule(rule)
    }

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

    override suspend fun getGoals(): List<PersonalGoal> {
        val uid = requireCurrentUserId()
        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_GOALS)
                .get()
                .awaitFirebase()

        return snapshot.documents
            .mapNotNull { it.toPersonalGoal(uid) }
            .sortedWith(compareBy<PersonalGoal> { it.status.name }.thenBy { it.deadlineAt })
    }

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

    override suspend fun updateGoal(goal: PersonalGoal) {
        insertGoal(goal)
    }

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

    override suspend fun getWallets(): List<PersonalWallet> {
        val uid = requireCurrentUserId()
        val snapshot =
            firestore
                .collection(COLLECTION_USER_PERSONAL)
                .document(uid)
                .collection(COLLECTION_WALLETS)
                .get()
                .awaitFirebase()

        return snapshot.documents
            .mapNotNull { it.toPersonalWallet(uid) }
            .sortedWith(compareBy<PersonalWallet> { it.isArchived }.thenBy { it.name.lowercase() })
    }

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

    override suspend fun updateWallet(wallet: PersonalWallet) {
        insertWallet(wallet)
    }

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

    private fun requireCurrentUserId(): String {
        return FirebaseProviders.auth.currentUser?.uid
            ?: throw IllegalStateException("Vui lòng đăng nhập để sử dụng dữ liệu cá nhân")
    }

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

    private fun DocumentSnapshot.getNumberDouble(field: String): Double? {
        return (get(field) as? Number)?.toDouble()
    }

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

    private suspend fun <T> Task<T>.awaitFirebase(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Tác vụ Firebase thất bại")
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

        @Volatile
        private var INSTANCE: FirebasePersonalRepository? = null

        fun getInstance(context: Context): FirebasePersonalRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebasePersonalRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

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
}
