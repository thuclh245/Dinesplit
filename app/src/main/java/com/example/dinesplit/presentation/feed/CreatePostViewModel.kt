package com.example.dinesplit.presentation.feed

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Story
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

enum class CreatePostMode {
    POST,
    STORY,
}

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val feedRepository = AppContainer.feedRepository()
    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val getCurrentUserProfileUseCase = AppContainer.getCurrentUserProfileUseCase(application)
    private val firestore = FirebaseProviders.firestore
    private val splitRepository = AppContainer.splitRepository(application)

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

    private val _postMode = MutableStateFlow(CreatePostMode.POST)
    val postMode: StateFlow<CreatePostMode> = _postMode.asStateFlow()

    private val _isLoadingExistingPost = MutableStateFlow(false)
    val isLoadingExistingPost: StateFlow<Boolean> = _isLoadingExistingPost.asStateFlow()

    private val _userGroups = MutableStateFlow<List<Group>>(emptyList())
    val userGroups: StateFlow<List<Group>> = _userGroups.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _selectedBillId = MutableStateFlow<String?>(null)
    val selectedBillId: StateFlow<String?> = _selectedBillId.asStateFlow()

    private val _selectedBillName = MutableStateFlow<String?>(null)
    val selectedBillName: StateFlow<String?> = _selectedBillName.asStateFlow()

    private val _availableBills = MutableStateFlow<List<Bill>>(emptyList())
    val availableBills: StateFlow<List<Bill>> = _availableBills.asStateFlow()

    private var observeBillsJob: kotlinx.coroutines.Job? = null
    private var currentPostId: String? = null

    val isFormValid: StateFlow<Boolean> = combine(
        _imageUri,
        _restaurantName,
        _caption,
        _postMode
    ) { image, restaurant, captionText, mode ->
        image != null && captionText.trim().length >= 3 &&
            (mode == CreatePostMode.STORY || restaurant.isNotBlank())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var lastSubmit: SubmitDraft? = null

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            splitRepository.getGroups().collect { groups ->
                _userGroups.value = groups
            }
        }
    }

    fun selectGroup(groupId: String?) {
        _selectedGroupId.value = groupId
        _selectedBillId.value = null
        _selectedBillName.value = null
        _availableBills.value = emptyList()
        observeBillsJob?.cancel()
        if (groupId != null) {
            observeBillsJob = viewModelScope.launch {
                splitRepository.getBills(groupId).collect { bills ->
                    _availableBills.value = bills
                    val currentBillId = _selectedBillId.value
                    if (currentBillId != null) {
                        val matchingBill = bills.firstOrNull { it.id == currentBillId }
                        if (matchingBill != null) {
                            _selectedBillName.value = matchingBill.name
                        }
                    }
                }
            }
        }
    }

    fun selectBill(billId: String?, billName: String?) {
        _selectedBillId.value = billId
        _selectedBillName.value = billName
    }

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

    fun updatePostMode(value: CreatePostMode) {
        if (currentPostId != null) return
        _postMode.value = value
        if (value == CreatePostMode.STORY) {
            _selectedGroupId.value = null
            _selectedBillId.value = null
            _selectedBillName.value = null
            _availableBills.value = emptyList()
            observeBillsJob?.cancel()
        }
    }

    fun resetUiState() {
        _uiState.value = CreatePostUiState.Idle
        _imageUri.value = null
        _restaurantName.value = ""
        _caption.value = ""
        _visibility.value = "public"
        _postMode.value = CreatePostMode.POST
        _isLoadingExistingPost.value = false
        currentPostId = null
        _selectedGroupId.value = null
        _selectedBillId.value = null
        _selectedBillName.value = null
        _availableBills.value = emptyList()
        observeBillsJob?.cancel()
    }

    fun initializePostMode(postId: String?, initialMode: CreatePostMode = CreatePostMode.POST) {
        if (postId.isNullOrBlank()) {
            resetUiState()
            updatePostMode(initialMode)
            loadGroups()
            return
        }
        currentPostId = postId
        _postMode.value = CreatePostMode.POST
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
                    
                    viewModelScope.launch(Dispatchers.Main) {
                        selectGroup(post.linkedGroupId)
                        selectBill(post.linkedBillId, null)
                    }
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
        val mode = _postMode.value

        lastSubmit = SubmitDraft(imgUri, restName, capt)

        if (imgUri == null) {
            _uiState.value = CreatePostUiState.Error("Please select a photo")
            return
        }
        if (mode == CreatePostMode.POST && restName.isBlank()) {
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

                if (mode == CreatePostMode.STORY && currentPostId == null) {
                    val storyId = UUID.randomUUID().toString()
                    val finalImageUrl = if (imgUri.toString().startsWith("content://") || imgUri.toString().startsWith("file://")) {
                        feedRepository.uploadStoryImage(storyId, imgUri)
                    } else {
                        imgUri.toString()
                    }
                    val createdAt = java.util.Date()
                    val story = Story(
                        id = storyId,
                        authorUid = session.uid,
                        authorName = displayName,
                        authorAvatar = profile?.avatarUrl.orEmpty(),
                        caption = capt.trim(),
                        imageUrl = finalImageUrl,
                        location = restName.trim().takeIf { it.isNotBlank() },
                        visibility = vis,
                        createdAt = createdAt,
                        expiresAt = java.util.Date(createdAt.time + 24L * 60 * 60 * 1000),
                    )
                    feedRepository.createStory(story)
                    _uiState.value = CreatePostUiState.Success
                    return@launch
                }

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
                    linkedGroupId = _selectedGroupId.value,
                    linkedBillId = _selectedBillId.value,
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
