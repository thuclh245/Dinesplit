package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.AppStartDestination
import com.example.dinesplit.domain.repository.AuthRepository

class ResolveStartDestinationUseCase(
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppStartDestination {
        val session = observeSessionUseCase().value ?: return AppStartDestination.AUTH
        return try {
            val profile = getCurrentUserProfileUseCase(session.uid)
            if (profile == null) {
                // Profile document genuinely does not exist in Firestore.
                // Sign out of orphan auth session to force user back to Login/Register screen.
                authRepository.logout()
                AppStartDestination.AUTH
            } else if (profile.isComplete()) {
                AppStartDestination.MAIN
            } else {
                AppStartDestination.COMPLETE_PROFILE
            }
        } catch (e: Exception) {
            // Temporary network/Firestore glitch. Fallback to MAIN and let the app handle offline/retry.
            android.util.Log.w("ResolveStartDestUseCase", "Failed to fetch profile during startup, falling back to MAIN", e)
            AppStartDestination.MAIN
        }
    }
}
