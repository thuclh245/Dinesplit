package com.example.dinesplit.presentation.feed

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Post
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.SharingStarted

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val feedRepository = AppContainer.feedRepository()
    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val getCurrentUserProfileUseCase = AppContainer.getCurrentUserProfileUseCase(application)
    private val firestore = FirebaseProviders.firestore

    private val _uiState = MutableStateFlow<CreatePostUiState>(CreatePostUiState.Idle)
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()

    private val _restaurantName = MutableStateFlow("")
    val restaurantName: StateFlow<String> = _restaurantName.asStateFlow()

    private val _caption = MutableStateFlow("")
    val caption: StateFlow<String> = _caption.asStateFlow()

    private val _visibility = MutableStateFlow("public")
    val visibility: StateFlow<String> = _visibility.asStateFlow()

    private val _isLoadingExistingPost = MutableStateFlow(false)
    val isLoadingExistingPost: StateFlow<Boolean> = _isLoadingExistingPost.asStateFlow()

    private var currentPostId: String? = null

    val isFormValid: StateFlow<Boolean> = combine(
        _imageUri,
        _restaurantName,
        _caption
    ) { image, restaurant, captionText ->
        image != null && restaurant.isNotBlank() && captionText.trim().length >= 3
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var lastSubmit: SubmitDraft? = null

    fun updateImageUri(value: Uri?) {
        _imageUri.value = value
    }

    fun updateRestaurantName(value: String) {
        _restaurantName.value = value
    }

    fun updateCaption(value: String) {
        _caption.value = value
    }

    fun updateVisibility(value: String) {
        _visibility.value = value
    }

    fun resetUiState() {
        _uiState.value = CreatePostUiState.Idle
        _imageUri.value = null
        _restaurantName.value = ""
        _caption.value = ""
        _visibility.value = "public"
        _isLoadingExistingPost.value = false
        currentPostId = null
    }

    fun initializePostMode(postId: String?) {
        if (postId.isNullOrBlank()) {
            resetUiState()
            return
        }
        currentPostId = postId
        _isLoadingExistingPost.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val document = firestore.collection("posts").document(postId).get().awaitFirebase()
                val post = document.toObject(Post::class.java)?.copy(id = document.id)
                if (post != null) {
                    _restaurantName.value = post.location.orEmpty()
                    _caption.value = post.caption
                    _imageUri.value = post.imageUrls.firstOrNull()?.let { Uri.parse(it) }
                    _visibility.value = post.visibility
                }
                _isLoadingExistingPost.value = false
            } catch (e: Exception) {
                _isLoadingExistingPost.value = false
                _uiState.value = CreatePostUiState.Error(FirebaseErrorMapper.toUserMessage(e))
            }
        }
    }

    fun retryLastSubmit() {
        lastSubmit?.let { draft ->
            submitPost(draft.imageUri, draft.restaurantName, draft.caption)
        }
    }

    fun submitPost(imageUri: Uri?, restaurantName: String, caption: String) {
        updateImageUri(imageUri)
        updateRestaurantName(restaurantName)
        updateCaption(caption)
        submitPost()
    }

    fun submitPost() {
        val imgUri = _imageUri.value
        val restName = _restaurantName.value
        val capt = _caption.value
        val vis = _visibility.value

        lastSubmit = SubmitDraft(imgUri, restName, capt)

        if (imgUri == null) {
            _uiState.value = CreatePostUiState.Error("Please select a photo")
            return
        }
        if (restName.isBlank()) {
            _uiState.value = CreatePostUiState.Error("Please enter a restaurant")
            return
        }
        if (capt.trim().length < 3) {
            _uiState.value = CreatePostUiState.Error("Caption must be at least 3 characters")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = CreatePostUiState.Loading
            try {
                val session = observeSessionUseCase().value
                if (session == null) {
                    _uiState.value = CreatePostUiState.Error("Session expired. Please sign in again.")
                    return@launch
                }

                val profile = getCurrentUserProfileUseCase(session.uid)
                val displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                    ?: session.email.substringBefore('@')

                val postId = currentPostId ?: UUID.randomUUID().toString()
                val finalImageUrl = if (imgUri.toString().startsWith("content://") || imgUri.toString().startsWith("file://")) {
                    uploadPostImage(session.uid, postId, imgUri)
                } else {
                    imgUri.toString()
                }

                var originalPost: Post? = null
                if (currentPostId != null) {
                    val doc = firestore.collection("posts").document(currentPostId!!).get().awaitFirebase()
                    originalPost = doc.toObject(Post::class.java)
                }

                val post = Post(
                    id = postId,
                    authorUid = originalPost?.authorUid ?: session.uid,
                    authorName = originalPost?.authorName ?: displayName,
                    authorAvatar = originalPost?.authorAvatar ?: profile?.avatarUrl.orEmpty(),
                    location = restName.trim().takeIf { it.isNotBlank() },
                    imageUrls = listOf(finalImageUrl),
                    likesCount = originalPost?.likesCount ?: 0,
                    likedBy = originalPost?.likedBy ?: emptyList(),
                    commentsCount = originalPost?.commentsCount ?: 0,
                    sharesCount = originalPost?.sharesCount ?: 0,
                    caption = capt.trim(),
                    visibility = vis,
                    createdAt = originalPost?.createdAt ?: java.util.Date(),
                    updatedAt = java.util.Date()
                )

                if (currentPostId != null) {
                    feedRepository.updatePost(post)
                } else {
                    feedRepository.createPost(post)
                }
                _uiState.value = CreatePostUiState.Success
            } catch (throwable: Throwable) {
                _uiState.value = CreatePostUiState.Error(FirebaseErrorMapper.toUserMessage(throwable))
            }
        }
    }

    private suspend fun uploadPostImage(uid: String, postId: String, imageUri: Uri): String {
        val imageRef = FirebaseProviders.storage.reference.child("posts/$uid/$postId.jpg")
        imageRef.putFile(imageUri).awaitFirebase()
        return imageRef.downloadUrl.awaitFirebase().toString()
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

    private data class SubmitDraft(
        val imageUri: Uri?,
        val restaurantName: String,
        val caption: String
    )
}
