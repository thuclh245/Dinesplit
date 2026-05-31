package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.Comment
import com.example.dinesplit.domain.repository.FeedRepository

class AddCommentUseCase(
    private val repository: FeedRepository,
) {
    suspend operator fun invoke(
        postId: String,
        comment: Comment,
    ) = repository.addComment(postId, comment)
}
