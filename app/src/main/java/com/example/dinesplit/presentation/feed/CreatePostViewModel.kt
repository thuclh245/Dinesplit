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

    private val _uiState = MutableStateFlow<CreatePostUiState>(CreatePostUiState.Idle)
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()

    private val _restaurantName = MutableStateFlow("")
    val restaurantName: StateFlow<String> = _restaurantName.asStateFlow()

    private val _caption = MutableStateFlow("")
    val caption: StateFlow<String> = _caption.asStateFlow()

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

    fun retryLastSubmit() {
        lastSubmit?.let { draft ->
            submitPost(draft.imageUri, draft.restaurantName, draft.caption)
        }
    }

    fun submitPost(imageUri: Uri?, restaurantName: String, caption: String) {
        updateImageUri(imageUri)
        updateRestaurantName(restaurantName)
        updateCaption(caption)
        lastSubmit = SubmitDraft(imageUri, restaurantName, caption)

        if (imageUri == null) {
            _uiState.value = CreatePostUiState.Error("Please select a photo")
            return
        }
        if (restaurantName.isBlank()) {
            _uiState.value = CreatePostUiState.Error("Please enter a restaurant")
            return
        }
        if (caption.trim().length < 3) {
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

                val postId = UUID.randomUUID().toString()
                val imageUrl = uploadPostImage(session.uid, postId, imageUri)
                val now = System.currentTimeMillis()

                val post = Post(
                    id = postId,
                    userId = session.uid,
                    userName = displayName,
                    userAvatarUrl = profile?.avatarUrl?.takeIf { it.isNotBlank() },
                    location = restaurantName.trim().takeIf { it.isNotBlank() },
                    mainImageUrl = imageUrl,
                    dinersCount = 1,
                    likesCount = 0,
                    commentsCount = 0,
                    caption = caption.trim(),
                    shareAmount = 0.0,
                    createdAt = now
                )

                feedRepository.createPost(post)
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
