package com.example.dinesplit.core.common

import android.content.Context
import com.example.dinesplit.data.repository.FirebaseAuthRepository
import com.example.dinesplit.data.repository.FirebaseProfileRepository
import com.example.dinesplit.domain.repository.AuthRepository
import com.example.dinesplit.domain.repository.ProfileRepository
import com.example.dinesplit.domain.usecase.GetCurrentUserProfileUseCase
import com.example.dinesplit.domain.usecase.LoginUseCase
import com.example.dinesplit.domain.usecase.LogoutUseCase
import com.example.dinesplit.domain.usecase.ObserveSessionUseCase
import com.example.dinesplit.domain.usecase.RegisterUseCase
import com.example.dinesplit.domain.usecase.ResolveStartDestinationUseCase
import com.example.dinesplit.domain.usecase.UpdateProfileUseCase

object AppContainer {
    fun authRepository(context: Context): AuthRepository {
        return FirebaseAuthRepository.getInstance(context)
    }

    fun profileRepository(context: Context): ProfileRepository {
        return FirebaseProfileRepository.getInstance(context)
    }

    fun loginUseCase(context: Context): LoginUseCase {
        return LoginUseCase(authRepository(context))
    }

    fun registerUseCase(context: Context): RegisterUseCase {
        return RegisterUseCase(authRepository(context))
    }

    fun observeSessionUseCase(context: Context): ObserveSessionUseCase {
        return ObserveSessionUseCase(authRepository(context))
    }

    fun logoutUseCase(context: Context): LogoutUseCase {
        return LogoutUseCase(authRepository(context))
    }

    fun getCurrentUserProfileUseCase(context: Context): GetCurrentUserProfileUseCase {
        return GetCurrentUserProfileUseCase(profileRepository(context))
    }

    fun updateProfileUseCase(context: Context): UpdateProfileUseCase {
        return UpdateProfileUseCase(profileRepository(context))
    }

    fun resolveStartDestinationUseCase(context: Context): ResolveStartDestinationUseCase {
        return ResolveStartDestinationUseCase(
            observeSessionUseCase = observeSessionUseCase(context),
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase(context)
        )
    }
}

