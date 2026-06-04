package com.example.dinesplit.presentation.feed.search

import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.UserProfile

data class SearchUiState(
    val query: String = "",
    val selectedFilter: SearchFilter = SearchFilter.All,

    val recentSearches: List<String> = emptyList(),
    val suggestedPeople: List<UserProfile> = emptyList(),
    val trendingPlaces: List<PlaceUiModel> = emptyList(),

    val postResults: List<Post> = emptyList(),
    val peopleResults: List<UserProfile> = emptyList(),
    val placeResults: List<PlaceUiModel> = emptyList(),

    val myFollowingIds: Set<String> = emptySet(),
    val myFollowerIds: Set<String> = emptySet(),

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasSearched: Boolean = false
) {
    val isExploreMode: Boolean get() = query.isBlank()

    val hasNoResult: Boolean get() = hasSearched &&
            postResults.isEmpty() &&
            peopleResults.isEmpty() &&
            placeResults.isEmpty()
}
