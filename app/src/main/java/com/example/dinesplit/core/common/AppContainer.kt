package com.example.dinesplit.core.common

import android.content.Context
import com.example.dinesplit.data.repository.*
import com.example.dinesplit.domain.repository.*
import com.example.dinesplit.domain.usecase.*
import com.google.firebase.firestore.FirebaseFirestore

object AppContainer {
    private var feedRepositoryInstance: FeedRepository? = null

    fun authRepository(context: Context): AuthRepository {
        return FirebaseAuthRepository.getInstance(context)
    }

    fun profileRepository(context: Context): ProfileRepository {
        return FirebaseProfileRepository.getInstance(context)
    }

    fun personalRepository(context: Context): PersonalRepository {
        return FirebasePersonalRepository.getInstance(context)
    }

    fun notificationRepository(context: Context): com.example.dinesplit.domain.repository.NotificationRepository {
        return com.example.dinesplit.data.repository.FirebaseNotificationRepository.getInstance(context)
    }

    fun feedRepository(): FeedRepository {
        return feedRepositoryInstance ?: synchronized(this) {
            feedRepositoryInstance ?: FirebaseFeedRepository(FirebaseFirestore.getInstance()).also {
                feedRepositoryInstance = it
            }
        }
    }

    fun splitRepository(): SplitRepository {
        return FirebaseSplitRepository(FirebaseFirestore.getInstance())
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

    fun uploadAvatarUseCase(context: Context): UploadAvatarUseCase {
        return UploadAvatarUseCase(profileRepository(context))
    }

    fun getFeedUseCase(): GetFeedUseCase {
        return GetFeedUseCase(feedRepository())
    }

    fun likePostUseCase(): LikePostUseCase {
        return LikePostUseCase(feedRepository())
    }

    fun unlikePostUseCase(): UnlikePostUseCase {
        return UnlikePostUseCase(feedRepository())
    }

    fun getGroupsUseCase(): GetGroupsUseCase {
        return GetGroupsUseCase(splitRepository())
    }

    fun getLinkedBillSummaryUseCase(): GetLinkedBillSummaryUseCase {
        return GetLinkedBillSummaryUseCase(splitRepository())
    }

    fun resolveStartDestinationUseCase(context: Context): ResolveStartDestinationUseCase {
        return ResolveStartDestinationUseCase(
            observeSessionUseCase = observeSessionUseCase(context),
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase(context),
            authRepository = authRepository(context),
        )
    }

    fun splitRepository(context: Context): SplitRepository {
        return FirebaseSplitRepository.getInstance(context)
    }

    fun qrPaymentRepository(context: Context): QrPaymentRepository {
        return FirebaseQrPaymentRepository.getInstance(context)
    }
}
