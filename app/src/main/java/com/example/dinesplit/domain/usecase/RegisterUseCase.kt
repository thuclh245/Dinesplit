package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.UserSession
import com.example.dinesplit.domain.repository.AuthRepository

class RegisterUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<UserSession> {
        return authRepository.register(email = email, password = password)
    }
}

