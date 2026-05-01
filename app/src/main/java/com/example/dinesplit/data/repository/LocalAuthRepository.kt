package com.example.dinesplit.data.repository

import android.content.Context
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

    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _sessionFlow = MutableStateFlow(loadCurrentSession())
    override val sessionFlow: StateFlow<UserSession?> = _sessionFlow.asStateFlow()

    override suspend fun login(email: String, password: String): Result<UserSession> {
        val normalizedEmail = normalizeEmail(email)
        val account = readAccount(normalizedEmail)
            ?: return Result.failure(IllegalArgumentException("Account not found"))

        if (account.password != password) {
            return Result.failure(IllegalArgumentException("Incorrect password"))
        }

        val session = UserSession(uid = account.uid, email = normalizedEmail)
        persistSession(session)
        _sessionFlow.value = session
        return Result.success(session)
    }

    override suspend fun register(email: String, password: String): Result<UserSession> {
        val normalizedEmail = normalizeEmail(email)
        if (readAccount(normalizedEmail) != null) {
            return Result.failure(IllegalArgumentException("Email is already registered"))
        }

        val uid = UUID.randomUUID().toString()
        saveAccount(Account(uid = uid, email = normalizedEmail, password = password))

        val session = UserSession(uid = uid, email = normalizedEmail)
        persistSession(session)
        _sessionFlow.value = session
        return Result.success(session)
    }

    override suspend fun logout() {
        prefs.edit()
            .remove(KEY_CURRENT_UID)
            .remove(KEY_CURRENT_EMAIL)
            .apply()
        _sessionFlow.value = null
    }

    private fun loadCurrentSession(): UserSession? {
        val uid = prefs.getString(KEY_CURRENT_UID, null)
        val email = prefs.getString(KEY_CURRENT_EMAIL, null)
        if (uid.isNullOrBlank() || email.isNullOrBlank()) return null
        return UserSession(uid = uid, email = email)
    }

    private fun persistSession(session: UserSession) {
        prefs.edit()
            .putString(KEY_CURRENT_UID, session.uid)
            .putString(KEY_CURRENT_EMAIL, session.email)
            .apply()
    }

    private fun saveAccount(account: Account) {
        prefs.edit().putString(accountKey(account.email), "${account.uid}|${account.password}").apply()
    }

    private fun readAccount(email: String): Account? {
        val rawValue = prefs.getString(accountKey(email), null) ?: return null
        val parts = rawValue.split("|", limit = 2)
        if (parts.size != 2) return null
        return Account(uid = parts[0], email = email, password = parts[1])
    }

    private fun accountKey(email: String): String = "account_$email"

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.ROOT)
    }

    private data class Account(
        val uid: String,
        val email: String,
        val password: String
    )

    companion object {
        private const val PREF_NAME = "dinesplit_auth"
        private const val KEY_CURRENT_UID = "current_uid"
        private const val KEY_CURRENT_EMAIL = "current_email"

        @Volatile
        private var INSTANCE: LocalAuthRepository? = null

        fun getInstance(context: Context): LocalAuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalAuthRepository(context = context).also { INSTANCE = it }
            }
        }
    }
}

