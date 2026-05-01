package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.repository.ProfileRepository

class UpdateProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profile: UserProfile): Result<Unit> {
        return profileRepository.upsertProfile(profile)
    }
}

