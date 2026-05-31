package com.example.dinesplit.domain.usecase

import android.net.Uri
import com.example.dinesplit.domain.repository.ProfileRepository

class UploadAvatarUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(
        uid: String,
        avatarUri: Uri,
    ): Result<String> {
        return profileRepository.uploadAvatar(uid, avatarUri)
    }
}
