package com.example.dinesplit.data.repository

import android.content.Context
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.SpendingReminder
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.repository.PersonalRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebasePersonalRepository private constructor(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val firestore: FirebaseFirestore = FirebaseProviders.firestore
) : PersonalRepository {

    override suspend fun getAllTransactions(): List<Transaction> {
        val uid = requireCurrentUserId()
        val snapshot = firestore
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

        val snapshot = firestore
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
        val snapshot = firestore
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

    private suspend fun ensureDefaultCategories(uid: String) {
        val categoriesRef = firestore
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
            ?: throw IllegalStateException("Please sign in to use Personal data")
    }

    private fun DocumentSnapshot.toTransaction(uid: String): Transaction? {
        val idValue = getString(FIELD_ID) ?: id
        val type = getString(FIELD_TYPE)?.let { value ->
            TransactionType.entries.firstOrNull { it.name == value }
        } ?: return null

        return Transaction(
            id = idValue,
            userId = getString(FIELD_USER_ID) ?: uid,
            amount = getNumberDouble(FIELD_AMOUNT) ?: return null,
            type = type,
            categoryId = getString(FIELD_CATEGORY_ID) ?: return null,
            category = getString(FIELD_CATEGORY) ?: return null,
            note = getString(FIELD_NOTE)?.takeIf { it.isNotBlank() },
            date = getLong(FIELD_DATE) ?: return null,
            createdAt = getLong(FIELD_CREATED_AT) ?: 0L
        )
    }

    private fun DocumentSnapshot.toStoredCategory(): StoredCategory? {
        val type = getString(FIELD_TYPE)?.let { value ->
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
            isActive = getBoolean(FIELD_IS_ACTIVE) ?: false
        )
    }

    private fun DocumentSnapshot.toSpendingReminder(): SpendingReminder? {
        val type = getString(FIELD_REMINDER_TYPE)?.let { value ->
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
            lastAlertedAt = getLong(FIELD_LAST_ALERTED_AT),
            createdAt = getLong(FIELD_CREATED_AT) ?: 0L,
            updatedAt = getLong(FIELD_UPDATED_AT) ?: 0L
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
            FIELD_UPDATED_AT to now
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
            FIELD_UPDATED_AT to now
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
            FIELD_UPDATED_AT to System.currentTimeMillis()
        )
    }

    private suspend fun <T> Task<T>.awaitFirebase(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase task failed")
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
                StoredCategory("c_other_income", "Other", "OT", TransactionType.INCOME, true, "Other incoming cash flows.", "0 VND", 0f, false)
            )
        }
    }
}
