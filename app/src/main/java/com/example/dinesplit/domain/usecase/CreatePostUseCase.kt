package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.repository.FeedRepository

class CreatePostUseCase(
    private val repository: FeedRepository
) {
    suspend operator fun invoke(post: Post) = repository.createPost(post)
}
