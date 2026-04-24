package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val sessionFlow: StateFlow<UserSession?>

    suspend fun login(email: String, password: String): Result<UserSession>

    suspend fun register(email: String, password: String): Result<UserSession>

    suspend fun logout()
}

