package com.example.dinesplit.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PersonalDatabaseHelper private constructor(
    context: Context
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE transactions (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                category_id TEXT NOT NULL,
                category TEXT NOT NULL,
                note TEXT,
                date_millis INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE categories (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                icon TEXT NOT NULL,
                type TEXT NOT NULL,
                is_custom INTEGER NOT NULL,
                description TEXT NOT NULL,
                amount_label TEXT NOT NULL,
                progress REAL NOT NULL,
                is_active INTEGER NOT NULL
            )
            """.trimIndent()
        )

        seedCategories(db)
        seedTransactions(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS transactions")
        db.execSQL("DROP TABLE IF EXISTS categories")
        onCreate(db)
    }

    private fun seedCategories(db: SQLiteDatabase) {
        val insertSql = """
            INSERT INTO categories (
                id, name, icon, type, is_custom, description, amount_label, progress, is_active
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val categories: List<Array<Any?>> = listOf(
            arrayOf<Any?>("c_food", "Dining Out", "FD", "EXPENSE", 0, "Restaurants, cafes, and delivery.", "$1,450.00", 0.65f, 1),
            arrayOf<Any?>("c_grocery", "Groceries", "GR", "EXPENSE", 0, "Supermarkets and local markets.", "$820.45", 0.40f, 0),
            arrayOf<Any?>("c_transit", "Transit", "TR", "EXPENSE", 0, "Rideshares and public transport.", "$340.00", 0f, 0),
            arrayOf<Any?>("c_fun", "Entertainment", "EN", "EXPENSE", 1, "Movies, events, and subscriptions.", "$210.50", 0f, 0),
            arrayOf<Any?>("c_salary", "Salary", "SL", "INCOME", 0, "Monthly fixed salary income.", "$3,500.00", 0.72f, 1),
            arrayOf<Any?>("c_bonus", "Bonus", "BN", "INCOME", 0, "Project and performance rewards.", "$750.00", 0.33f, 0),
            arrayOf<Any?>("c_gift", "Gift", "GF", "INCOME", 1, "Personal gifts and contributions.", "$220.00", 0f, 0),
            arrayOf<Any?>("c_other_income", "Other", "OT", "INCOME", 1, "Other incoming cash flows.", "$100.00", 0f, 0)
        )

        categories.forEach { row ->
            db.execSQL(insertSql, row)
        }
    }

    private fun seedTransactions(db: SQLiteDatabase) {
        val insertSql = """
            INSERT INTO transactions (
                id, user_id, amount, type, category_id, category, note, date_millis, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val transactions: List<Array<Any?>> = listOf(
            arrayOf<Any?>("tx_1", "user_1", 525000.0, "EXPENSE", "c_food", "Dining Out", "Dinner with team", epochMillis(4, 5), epochMillis(4, 5)),
            arrayOf<Any?>("tx_2", "user_1", 187500.0, "EXPENSE", "c_transit", "Transit", null, epochMillis(4, 4), epochMillis(4, 4)),
            arrayOf<Any?>("tx_3", "user_1", 3500000.0, "INCOME", "c_salary", "Salary", "Monthly salary", epochMillis(4, 1), epochMillis(4, 1)),
            arrayOf<Any?>("tx_4", "user_1", 220000.0, "EXPENSE", "c_grocery", "Groceries", null, epochMillis(3, 20), epochMillis(3, 20)),
            arrayOf<Any?>("tx_5", "user_1", 750000.0, "INCOME", "c_bonus", "Bonus", "Project reward", epochMillis(3, 15), epochMillis(3, 15))
        )

        transactions.forEach { row ->
            db.execSQL(insertSql, row)
        }
    }

    companion object {
        private const val DATABASE_NAME = "dinesplit_personal.db"
        private const val DATABASE_VERSION = 2

        @Volatile
        private var INSTANCE: PersonalDatabaseHelper? = null

        fun getInstance(context: Context): PersonalDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PersonalDatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun epochMillis(month: Int, day: Int): Long {
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, 2026)
                set(java.util.Calendar.MONTH, month - 1)
                set(java.util.Calendar.DAY_OF_MONTH, day)
                set(java.util.Calendar.HOUR_OF_DAY, 12)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }
    }
}

