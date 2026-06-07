package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.Comment
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.Story
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getFeedPosts(): Flow<List<Post>>

    fun getActiveStories(): Flow<List<Story>>

    fun getUserPosts(userId: String): Flow<List<Post>>

    suspend fun createPost(post: Post)

    suspend fun createStory(story: Story)

    suspend fun likePost(
        postId: String,
        userId: String,
    )

    suspend fun unlikePost(
        postId: String,
        userId: String,
    )

    suspend fun updatePost(post: Post)

    suspend fun deletePost(postId: String)

    suspend fun uploadPostImage(
        postId: String,
        imageUri: android.net.Uri,
    ): String

    suspend fun uploadStoryImage(
        storyId: String,
        imageUri: android.net.Uri,
    ): String

    suspend fun savePost(postId: String, userId: String)

    suspend fun unsavePost(postId: String, userId: String)

    fun getSavedPosts(userId: String): Flow<List<Post>>

    // Comments
    fun getComments(postId: String): Flow<List<Comment>>

    suspend fun addComment(
        postId: String,
        comment: Comment,
    )

    // Pagination
    suspend fun getFeedPostsBatch(
        limit: Long,
        lastPostId: String?,
    ): List<Post>

    suspend fun searchPosts(query: String): List<Post>
}
