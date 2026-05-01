package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.repository.ProfileRepository

class GetCurrentUserProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(uid: String): UserProfile? {
        return profileRepository.getProfile(uid)
    }
}

