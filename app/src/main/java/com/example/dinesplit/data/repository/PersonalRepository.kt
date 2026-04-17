package com.example.dinesplit.data.repository

import android.content.ContentValues
import android.content.Context
import com.example.dinesplit.data.local.PersonalDatabaseHelper
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType

data class StoredCategory(
    val id: String,
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

    fun getAllTransactions(): List<Transaction> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "transactions",
            arrayOf("id", "user_id", "amount", "type", "category_id", "category", "note", "date_millis", "created_at"),
            null,
            null,
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
                    type = TransactionType.valueOf(it.getString(3)),
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

    fun getCategories(): List<StoredCategory> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "categories",
            arrayOf(
                "id",
                "name",
                "icon",
                "type",
                "is_custom",
                "description",
                "amount_label",
                "progress",
                "is_active"
            ),
            null,
            null,
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
                    name = it.getString(1),
                    icon = it.getString(2),
                    type = TransactionType.valueOf(it.getString(3)),
                    isCustom = it.getInt(4) == 1,
                    description = it.getString(5),
                    amountLabel = it.getString(6),
                    progress = it.getFloat(7),
                    isActive = it.getInt(8) == 1
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
            "id = ?",
            arrayOf(category.id)
        )
    }

    fun deleteCategory(categoryId: String) {
        val db = dbHelper.writableDatabase
        db.delete(
            "categories",
            "id = ?",
            arrayOf(categoryId)
        )
    }

    private fun StoredCategory.toContentValues(): ContentValues {
        return ContentValues().apply {
            put("id", id)
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

