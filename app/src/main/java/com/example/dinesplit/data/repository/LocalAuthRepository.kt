package com.example.dinesplit.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.dinesplit.data.local.PersonalDatabaseHelper
import com.example.dinesplit.domain.model.UserSession
import com.example.dinesplit.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class LocalAuthRepository private constructor(
    context: Context
) : AuthRepository {

    private val dbHelper = PersonalDatabaseHelper.getInstance(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _sessionFlow = MutableStateFlow<UserSession?>(null)
    override val sessionFlow: StateFlow<UserSession?> = _sessionFlow.asStateFlow()

    init {
        // ✅ Load persisted session on initialization
        loadPersistedSession()
    }

    private fun loadPersistedSession() {
        val uid = prefs.getString("uid", null) ?: return
        val email = prefs.getString("email", null) ?: return
        val session = UserSession(uid = uid, email = email)
        _sessionFlow.value = session
    }

    override suspend fun login(email: String, password: String): Result<UserSession> {
        val normalizedEmail = normalizeEmail(email)
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            "accounts",
            arrayOf("uid", "password_hash"),
            "email = ?",
            arrayOf(normalizedEmail),
            null,
            null,
            null
        )

        val account = if (cursor.moveToFirst()) {
            Account(
                uid = cursor.getString(0),
                email = normalizedEmail,
                passwordHash = cursor.getString(1)
            )
        } else {
            null
        }
        cursor.close()

        if (account == null) {
            return Result.failure(IllegalArgumentException("Account not found"))
        }

        if (!verifyPassword(password, account.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect password"))
        }

        val session = UserSession(uid = account.uid, email = normalizedEmail)
        _sessionFlow.value = session
        saveSessionToPrefs(session)
        return Result.success(session)
    }

    override suspend fun register(email: String, password: String): Result<UserSession> {
        val normalizedEmail = normalizeEmail(email)
        val db = dbHelper.readableDatabase

        // Check if email already exists
        val cursor = db.query(
            "accounts",
            arrayOf("uid"),
            "email = ?",
            arrayOf(normalizedEmail),
            null,
            null,
            null
        )
        val exists = cursor.moveToFirst()
        cursor.close()

        if (exists) {
            return Result.failure(IllegalArgumentException("Email is already registered"))
        }

        val uid = UUID.randomUUID().toString()
        val passwordHash = hashPassword(password)
        val writeDb = dbHelper.writableDatabase

        return try {
            // Insert into accounts
            val accountValues = android.content.ContentValues().apply {
                put("uid", uid)
                put("email", normalizedEmail)
                put("password_hash", passwordHash)
            }
            writeDb.insert("accounts", null, accountValues)

            // Insert into users (default profile)
            val now = System.currentTimeMillis()
            val userValues = android.content.ContentValues().apply {
                put("uid", uid)
                put("email", normalizedEmail)
                put("display_name", "")
                put("username", "")
                put("avatar_url", "")
                put("bio", "")
                put("created_at", now)
                put("updated_at", now)
            }
            writeDb.insert("users", null, userValues)

            val session = UserSession(uid = uid, email = normalizedEmail)
            _sessionFlow.value = session
            saveSessionToPrefs(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        _sessionFlow.value = null
        prefs.edit().clear().apply()
    }

    private fun hashPassword(password: String): String {
        // SHA-256 hash
        return java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.ROOT)
    }

    private fun saveSessionToPrefs(session: UserSession) {
        with(prefs.edit()) {
            putString("uid", session.uid)
            putString("email", session.email)
            apply()
        }
    }

    private data class Account(
        val uid: String,
        val email: String,
        val passwordHash: String
    )

    companion object {

        @Volatile
        private var INSTANCE: LocalAuthRepository? = null

        fun getInstance(context: Context): LocalAuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalAuthRepository(context = context).also { INSTANCE = it }
            }
        }
    }
}
