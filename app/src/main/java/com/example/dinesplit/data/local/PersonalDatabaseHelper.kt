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
                user_id TEXT NOT NULL,
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

        db.execSQL(
            """
            CREATE TABLE accounts (
                uid TEXT PRIMARY KEY,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE users (
                uid TEXT PRIMARY KEY,
                email TEXT NOT NULL UNIQUE,
                display_name TEXT NOT NULL,
                username TEXT NOT NULL,
                avatar_url TEXT,
                bio TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(uid) REFERENCES accounts(uid)
            )
            """.trimIndent()
        )

        seedCategories(db)
        // ✅ seedTransactions(db) has been removed to ensure a clean start for new users
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // ✅ If version is increased to 5, drop everything for a fresh start
        if (oldVersion < 5) {
            db.execSQL("DROP TABLE IF EXISTS transactions")
            db.execSQL("DROP TABLE IF EXISTS categories")
            db.execSQL("DROP TABLE IF EXISTS accounts")
            db.execSQL("DROP TABLE IF EXISTS users")
            onCreate(db)
        }
    }

    private fun seedCategories(db: SQLiteDatabase) {
        val insertSql = """
            INSERT INTO categories (
                id, user_id, name, icon, type, is_custom, description, amount_label, progress, is_active
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val categories: List<Array<Any?>> = listOf(
            arrayOf<Any?>("c_food", "global", "Dining Out", "FD", "EXPENSE", 0, "Restaurants, cafes, and delivery.", "$0.00", 0f, 1),
            arrayOf<Any?>("c_grocery", "global", "Groceries", "GR", "EXPENSE", 0, "Supermarkets and local markets.", "$0.00", 0f, 0),
            arrayOf<Any?>("c_transit", "global", "Transit", "TR", "EXPENSE", 0, "Rideshares and public transport.", "$0.00", 0f, 0),
            arrayOf<Any?>("c_fun", "global", "Entertainment", "EN", "EXPENSE", 1, "Movies, events, and subscriptions.", "$0.00", 0f, 0),
            arrayOf<Any?>("c_salary", "global", "Salary", "SL", "INCOME", 0, "Monthly fixed salary income.", "$0.00", 0f, 1),
            arrayOf<Any?>("c_bonus", "global", "Bonus", "BN", "INCOME", 0, "Project and performance rewards.", "$0.00", 0f, 0),
            arrayOf<Any?>("c_gift", "global", "Gift", "GF", "INCOME", 1, "Personal gifts and contributions.", "$0.00", 0f, 0),
            arrayOf<Any?>("c_other_income", "global", "Other", "OT", "INCOME", 1, "Other incoming cash flows.", "$0.00", 0f, 0)
        )

        categories.forEach { row ->
            db.execSQL(insertSql, row)
        }
    }

    companion object {
        private const val DATABASE_NAME = "dinesplit_personal.db"
        private const val DATABASE_VERSION = 5 // ✅ Increased to force clean up

        @Volatile
        private var INSTANCE: PersonalDatabaseHelper? = null

        fun getInstance(context: Context): PersonalDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PersonalDatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
