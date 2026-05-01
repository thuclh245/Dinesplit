package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.AppStartDestination

class ResolveStartDestinationUseCase(
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
) {
    suspend operator fun invoke(): AppStartDestination {
        val session = observeSessionUseCase().value ?: return AppStartDestination.AUTH
        val profile = getCurrentUserProfileUseCase(session.uid)
        return if (profile?.isComplete() == true) {
            AppStartDestination.MAIN
        } else {
            AppStartDestination.COMPLETE_PROFILE
        }
    }
}

