package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.repository.FeedRepository

class UnlikePostUseCase(
    private val repository: FeedRepository
) {
    suspend operator fun invoke(postId: String, userId: String) = 
        repository.unlikePost(postId, userId)
}
