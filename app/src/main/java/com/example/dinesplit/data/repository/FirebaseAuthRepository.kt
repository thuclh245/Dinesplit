package com.example.dinesplit.data.repository

import android.content.Context
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.UserSession
import com.example.dinesplit.domain.repository.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseAuthRepository private constructor(
    @Suppress("UNUSED_PARAMETER") context: Context,
) : AuthRepository {
    private val auth: FirebaseAuth = FirebaseProviders.auth

    private val _sessionFlow = MutableStateFlow(auth.currentUser?.toUserSession())
    override val sessionFlow: StateFlow<UserSession?> = _sessionFlow.asStateFlow()

    private val authStateListener =
        FirebaseAuth.AuthStateListener { firebaseAuth: FirebaseAuth ->
            _sessionFlow.value = firebaseAuth.currentUser?.toUserSession()
        }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override suspend fun login(
        email: String,
        password: String,
    ): Result<UserSession> {
        return runCatching {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).awaitFirebase()
            val session =
                result.toSession()
                    ?: error("Unable to resolve Firebase session")
            _sessionFlow.value = session
            session
        }
    }

    override suspend fun register(
        email: String,
        password: String,
    ): Result<UserSession> {
        return runCatching {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).awaitFirebase()
            val session =
                result.toSession()
                    ?: error("Unable to resolve Firebase session")
            _sessionFlow.value = session
            session
        }
    }

    override suspend fun logout() {
        auth.signOut()
        _sessionFlow.value = null
    }

    private fun AuthResult.toSession(): UserSession? {
        val user = user ?: auth.currentUser ?: return null
        return UserSession(uid = user.uid, email = user.email.orEmpty())
    }

    private fun FirebaseUser.toUserSession(): UserSession {
        return UserSession(uid = uid, email = email.orEmpty())
    }

    private suspend fun <T> Task<T>.awaitFirebase(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase task failed"),
                    )
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseAuthRepository? = null

        fun getInstance(context: Context): FirebaseAuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseAuthRepository(context = context).also { INSTANCE = it }
            }
        }
    }
}
