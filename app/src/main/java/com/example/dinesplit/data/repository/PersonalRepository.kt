package com.example.dinesplit.data.repository

import android.content.ContentValues
import android.content.Context
import com.example.dinesplit.data.local.PersonalDatabaseHelper
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.model.transactionTypeFromString

data class StoredCategory(
    val id: String,
    val userId: String,
    val name: String,
    val icon: String,
    val type: TransactionType,
    val isCustom: Boolean,
    val description: String,
    val amountLabel: String,
    val progress: Float,
    val isActive: Boolean
)

class PersonalRepository private constructor(
    context: Context
) {
    private val dbHelper = PersonalDatabaseHelper.getInstance(context)

    fun getTransactions(userId: String): List<Transaction> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "transactions",
            arrayOf("id", "user_id", "amount", "type", "category_id", "category", "note", "date_millis", "created_at"),
            "user_id = ?",
            arrayOf(userId),
            null,
            null,
            "date_millis DESC"
        )

        cursor.use {
            if (!it.moveToFirst()) return emptyList()
            val result = mutableListOf<Transaction>()
            do {
                result += Transaction(
                    id = it.getString(0),
                    userId = it.getString(1),
                    amount = it.getDouble(2),
                    type = transactionTypeFromString(it.getString(3)),
                    categoryId = it.getString(4),
                    category = it.getString(5),
                    note = it.getString(6),
                    date = it.getLong(7),
                    createdAt = it.getLong(8)
                )
            } while (it.moveToNext())
            return result
        }
    }

    fun getCategories(userId: String): List<StoredCategory> {
        val db = dbHelper.readableDatabase
        // ✅ Get both global categories and user-specific ones
        val cursor = db.query(
            "categories",
            arrayOf("id", "user_id", "name", "icon", "type", "is_custom", "description", "amount_label", "progress", "is_active"),
            "user_id = ? OR user_id = ?",
            arrayOf("global", userId),
            null,
            null,
            "name ASC"
        )

        cursor.use {
            if (!it.moveToFirst()) return emptyList()
            val result = mutableListOf<StoredCategory>()
            do {
                result += StoredCategory(
                    id = it.getString(0),
                    userId = it.getString(1),
                    name = it.getString(2),
                    icon = it.getString(3),
                    type = transactionTypeFromString(it.getString(4)),
                    isCustom = it.getInt(5) == 1,
                    description = it.getString(6),
                    amountLabel = it.getString(7),
                    progress = it.getFloat(8),
                    isActive = it.getInt(9) == 1
                )
            } while (it.moveToNext())
            return result
        }
    }

    fun insertTransaction(transaction: Transaction) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", transaction.id)
            put("user_id", transaction.userId)
            put("amount", transaction.amount)
            put("type", transaction.type.name)
            put("category_id", transaction.categoryId)
            put("category", transaction.category)
            put("note", transaction.note)
            put("date_millis", transaction.date)
            put("created_at", transaction.createdAt)
        }
        db.insert("transactions", null, values)
    }

    fun insertCategory(category: StoredCategory) {
        val db = dbHelper.writableDatabase
        db.insert("categories", null, category.toContentValues())
    }

    fun updateCategory(category: StoredCategory) {
        val db = dbHelper.writableDatabase
        db.update(
            "categories",
            category.toContentValues(),
            "id = ? AND user_id != 'global'", // ✅ Protect global categories
            arrayOf(category.id)
        )
    }

    fun deleteCategory(categoryId: String, userId: String) {
        val db = dbHelper.writableDatabase
        db.delete(
            "categories",
            "id = ? AND user_id = ?", // ✅ Only allow deleting own categories
            arrayOf(categoryId, userId)
        )
    }


    private fun StoredCategory.toContentValues(): ContentValues {
        return ContentValues().apply {
            put("id", id)
            put("user_id", userId)
            put("name", name)
            put("icon", icon)
            put("type", type.name)
            put("is_custom", if (isCustom) 1 else 0)
            put("description", description)
            put("amount_label", amountLabel)
            put("progress", progress)
            put("is_active", if (isActive) 1 else 0)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PersonalRepository? = null
        fun getInstance(context: Context): PersonalRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PersonalRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
