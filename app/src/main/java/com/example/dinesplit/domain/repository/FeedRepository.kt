package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getFeedPosts(): Flow<List<Post>>
    suspend fun createPost(post: Post)
    suspend fun likePost(postId: String)
}
