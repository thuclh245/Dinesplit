package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow

class GetFeedUseCase(
    private val repository: FeedRepository
) {
    operator fun invoke(): Flow<List<Post>> = repository.getFeedPosts()
}
