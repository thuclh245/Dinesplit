package com.example.dinesplit.data.repository

import com.example.dinesplit.domain.model.Comment
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.repository.FeedRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(Post::class.java)?.copy(id = doc.id)
                    }.getOrNull()
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { subscription.remove() }
    }

    override fun getUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val subscription = firestore.collection("posts")
            .whereEqualTo("authorUid", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(Post::class.java)?.copy(id = doc.id)
                    }.getOrNull()
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createPost(post: Post) {
        firestore.collection("posts").document(post.id).set(post).awaitFirebase()
    }

    override suspend fun likePost(postId: String, userId: String) {
        val postRef = firestore.collection("posts").document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikes = snapshot.getLong("likesCount") ?: 0L
            transaction.update(postRef, "likesCount", currentLikes + 1)
        }.awaitFirebase()
    }

    override suspend fun unlikePost(postId: String, userId: String) {
        val postRef = firestore.collection("posts").document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikes = snapshot.getLong("likesCount") ?: 0L
            val newLikes = if (currentLikes > 0) currentLikes - 1 else 0L
            transaction.update(postRef, "likesCount", newLikes)
        }.awaitFirebase()
    }

    override fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val subscription = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(Comment::class.java)?.copy(id = doc.id)
                    }.getOrNull()
                } ?: emptyList()
                trySend(comments)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addComment(postId: String, comment: Comment) {
        val postRef = firestore.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()
        val finalComment = comment.copy(id = commentRef.id, createdAt = java.util.Date())
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentComments = snapshot.getLong("commentsCount") ?: 0L
            transaction.set(commentRef, finalComment)
            transaction.update(postRef, "commentsCount", currentComments + 1)
        }.awaitFirebase()
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

