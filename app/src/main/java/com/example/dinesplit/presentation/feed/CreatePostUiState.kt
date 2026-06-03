package com.example.dinesplit.presentation.feed

sealed interface CreatePostUiState {
    data object Idle : CreatePostUiState
    data object Loading : CreatePostUiState
    data object Success : CreatePostUiState
    data class Error(val message: String) : CreatePostUiState
}
