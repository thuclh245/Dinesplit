package com.example.dinesplit.data.repository

import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.repository.FeedRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseFeedRepository(
    private val firestore: FirebaseFirestore
) : FeedRepository {

    override fun getFeedPosts(): Flow<List<Post>> = callbackFlow {
        val subscription = firestore.collection("posts")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    // Map Firestore document to Post model
                    null // Placeholder for actual mapping logic
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createPost(post: Post) {
        firestore.collection("posts").document(post.id).set(post).awaitFirebase()
    }

    override suspend fun likePost(postId: String) {
        // Implementation for liking a post
    }

    private suspend fun <T> Task<T>.awaitFirebase(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase task failed")
                    )
                }
            }
        }
    }
}
