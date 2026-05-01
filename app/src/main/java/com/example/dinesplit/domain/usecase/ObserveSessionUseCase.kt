package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.UserSession
import com.example.dinesplit.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveSessionUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): StateFlow<UserSession?> {
        return authRepository.sessionFlow
    }
}

