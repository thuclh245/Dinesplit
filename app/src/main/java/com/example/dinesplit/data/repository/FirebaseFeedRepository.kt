package com.example.dinesplit.data.repository

import android.net.Uri
import com.example.dinesplit.core.firebase.FirebaseProviders
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
    private val firestore: FirebaseFirestore,
) : FeedRepository {
    override fun getFeedPosts(): Flow<List<Post>> =
        callbackFlow {
            val subscription =
                firestore.collection("posts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val posts =
                            snapshot?.documents?.mapNotNull { doc ->
                                runCatching {
                                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                                }.getOrNull()
                            } ?: emptyList()
                        trySend(posts)
                    }
            awaitClose { subscription.remove() }
        }

    override fun getUserPosts(userId: String): Flow<List<Post>> =
        callbackFlow {
            val subscription =
                firestore.collection("posts")
                    .whereEqualTo("authorUid", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val posts =
                            snapshot?.documents?.mapNotNull { doc ->
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

    override suspend fun updatePost(post: Post) {
        firestore.collection("posts").document(post.id).set(post).awaitFirebase()
    }

    override suspend fun deletePost(postId: String) {
        firestore.collection("posts").document(postId).delete().awaitFirebase()
    }

    override suspend fun uploadPostImage(
        postId: String,
        imageUri: Uri,
    ): String {
        val storageRef = FirebaseProviders.storage.reference.child("posts/$postId/post_image.jpg")
        storageRef.putFile(imageUri).awaitFirebase()
        return storageRef.downloadUrl.awaitFirebase().toString()
    }

    override suspend fun likePost(
        postId: String,
        userId: String,
    ) {
        val postRef = firestore.collection("posts").document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikedBy = (snapshot.get("likedBy") as? List<String>) ?: emptyList()
            if (!currentLikedBy.contains(userId)) {
                val newLikedBy = currentLikedBy + userId
                val newLikesCount = newLikedBy.size.toLong()
                transaction.update(postRef, "likesCount", newLikesCount, "likedBy", newLikedBy)

                // Write notification inside transaction
                val authorUid = snapshot.getString("authorUid")
                if (!authorUid.isNullOrBlank() && authorUid != userId) {
                    val userRef = firestore.collection("users").document(userId)
                    val userSnapshot = transaction.get(userRef)
                    val triggeredByUserName = userSnapshot.getString("displayName") ?: "Ai đó"
                    val postTitle = snapshot.getString("caption")?.take(30) ?: "bài viết"

                    val notificationId = "${System.currentTimeMillis()}_$postId"
                    val notificationRef = firestore.collection("user_notifications")
                        .document(authorUid)
                        .collection("notifications")
                        .document(notificationId)

                    val notificationMap = mapOf(
                        "id" to notificationId,
                        "userId" to authorUid,
                        "title" to "$triggeredByUserName đã thích bài viết của bạn",
                        "subtitle" to postTitle,
                        "type" to "ACTIVITY_UPDATE",
                        "relatedId" to postId,
                        "isRead" to false,
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis(),
                        "deepLinkDestination" to "ACTIVITY_DETAIL",
                        "deepLinkTargetId" to postId
                    )
                    transaction.set(notificationRef, notificationMap)
                }
            }
        }.awaitFirebase()
    }

    override suspend fun unlikePost(
        postId: String,
        userId: String,
    ) {
        val postRef = firestore.collection("posts").document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikedBy = (snapshot.get("likedBy") as? List<String>) ?: emptyList()
            if (currentLikedBy.contains(userId)) {
                val newLikedBy = currentLikedBy - userId
                val newLikesCount = newLikedBy.size.toLong()
                transaction.update(postRef, "likesCount", newLikesCount, "likedBy", newLikedBy)
            }
        }.awaitFirebase()
    }

    override fun getComments(postId: String): Flow<List<Comment>> =
        callbackFlow {
            val subscription =
                firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val comments =
                            snapshot?.documents?.mapNotNull { doc ->
                                runCatching {
                                    doc.toObject(Comment::class.java)?.copy(id = doc.id)
                                }.getOrNull()
                            } ?: emptyList()
                        trySend(comments)
                    }
            awaitClose { subscription.remove() }
        }

    override suspend fun addComment(
        postId: String,
        comment: Comment,
    ) {
        val postRef = firestore.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()
        val finalComment = comment.copy(id = commentRef.id, createdAt = java.util.Date())

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentComments = snapshot.getLong("commentsCount") ?: 0L
            val authorUid = snapshot.getString("authorUid")

            transaction.set(commentRef, finalComment)
            transaction.update(postRef, "commentsCount", currentComments + 1)

            // Write notification inside transaction
            if (!authorUid.isNullOrBlank() && authorUid != comment.authorUid) {
                val postTitle = snapshot.getString("caption")?.take(30) ?: "bài viết"
                val notificationId = "${System.currentTimeMillis()}_$postId"
                val notificationRef = firestore.collection("user_notifications")
                    .document(authorUid)
                    .collection("notifications")
                    .document(notificationId)

                val notificationMap = mapOf(
                    "id" to notificationId,
                    "userId" to authorUid,
                    "title" to "${comment.authorName} đã bình luận về bài viết của bạn",
                    "subtitle" to finalComment.content.take(50),
                    "type" to "ACTIVITY_UPDATE",
                    "relatedId" to postId,
                    "isRead" to false,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "deepLinkDestination" to "ACTIVITY_DETAIL",
                    "deepLinkTargetId" to postId
                )
                transaction.set(notificationRef, notificationMap)
            }
        }.awaitFirebase()
    }

    override suspend fun getFeedPostsBatch(
        limit: Long,
        lastPostId: String?,
    ): List<Post> {
        var query = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)

        if (!lastPostId.isNullOrBlank()) {
            val lastDocSnapshot = firestore.collection("posts")
                .document(lastPostId)
                .get()
                .awaitFirebase()
            if (lastDocSnapshot.exists()) {
                query = query.startAfter(lastDocSnapshot)
            }
        }

        val snapshot = query.get().awaitFirebase()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }.getOrNull()
        }
    }

    override suspend fun searchPosts(query: String): List<Post> {
        val lowerQuery = query.lowercase().trim()
        val posts = getFeedPostsBatch(100, null)
        if (lowerQuery.isEmpty()) return posts
        return posts.filter { post ->
            post.caption.lowercase().contains(lowerQuery) ||
                post.location?.lowercase()?.contains(lowerQuery) == true ||
                post.authorName.lowercase().contains(lowerQuery)
        }
    }

    private suspend fun <T> Task<T>.awaitFirebase(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase task failed"),
                    )
                }
            }
        }
    }
}
