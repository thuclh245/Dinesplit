package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.Comment
import com.example.dinesplit.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getFeedPosts(): Flow<List<Post>>
    fun getUserPosts(userId: String): Flow<List<Post>>
    suspend fun createPost(post: Post)
    suspend fun likePost(postId: String, userId: String)
    suspend fun unlikePost(postId: String, userId: String)
    
    // Comments
    fun getComments(postId: String): Flow<List<Comment>>
    suspend fun addComment(postId: String, comment: Comment)
}
