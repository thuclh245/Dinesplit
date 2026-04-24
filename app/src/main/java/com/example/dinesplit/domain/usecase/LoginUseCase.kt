package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.UserSession
import com.example.dinesplit.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<UserSession> {
        return authRepository.login(email = email, password = password)
    }
}

