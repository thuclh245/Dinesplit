package com.example.dinesplit.presentation.feed.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository = AppContainer.profileRepository(application)
    private val feedRepository = AppContainer.feedRepository()
    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val firestore = FirebaseProviders.firestore

    // KỸ THUẬT DEBOUNCE CHUẨN SENIOR: Sử dụng luồng dòng chảy queryFlow độc lập
    private val queryFlow = MutableStateFlow("")

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    var currentUserId: String? = null
        private set

    init {
        // 1. Lắng nghe Session người dùng để tải dữ lịch sử và đề xuất Khám phá
        viewModelScope.launch {
            observeSessionUseCase().collect { session ->
                currentUserId = session?.uid
                loadExploreData()
                loadMyFollowRelations()
            }
        }

        // 2. KÍCH HOẠT RE-ACTIVE DEBOUNCE FLOW: Hoãn mạng 300ms tối ưu hóa gói băng thông đọc
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    performSearch(query)
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, errorMessage = null) }
        queryFlow.value = newQuery
    }

    fun onFilterChange(newFilter: SearchFilter) {
        _uiState.update { it.copy(selectedFilter = newFilter) }
        if (_uiState.value.query.isNotBlank()) {
            executeSearch()
        }
    }

    fun executeSearch() {
        // Cổng thủ công ép kích hoạt chạy lại tìm kiếm ngay lập tức (Nút Tìm / Nút Thử lại)
        queryFlow.value = _uiState.value.query
    }

    // TỐI ƯU HÓA SONG SONG (CONCURRENT ASYNC FLOW): Kích hoạt truy vấn đồng thời rút ngắn thời gian phản hồi UI
    private suspend fun performSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    peopleResults = emptyList(),
                    postResults = emptyList(),
                    placeResults = emptyList(),
                    errorMessage = null,
                    hasSearched = false
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val normalized = trimmed.lowercase()

        runCatching {
            coroutineScope {
                val userDeferred = async(Dispatchers.IO) { profileRepository.searchProfiles(trimmed) }
                val postDeferred = async(Dispatchers.IO) { feedRepository.searchPosts(trimmed) }

                val users = userDeferred.await()
                val posts = postDeferred.await()
                Triple(users, posts, buildPlaceResults(normalized, posts))
            }
        }.onSuccess { triple ->
            val users = triple.first.getOrDefault(emptyList())
            val posts = triple.second
            val places = triple.third

            _uiState.update {
                it.copy(
                    isLoading = false,
                    peopleResults = users,
                    postResults = posts,
                    placeResults = places,
                    hasSearched = true
                )
            }

            // Lưu vết lịch sử tìm kiếm cục bộ lên mây bảo mật
            currentUserId?.let { uid ->
                profileRepository.saveRecentSearch(uid, trimmed)
                loadRecentSearches()
            }
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                )
            }
        }
    }

    // EXPLORE MODE DATA EXTRACTION FLOW
    fun loadExploreData() {
        loadRecentSearches()
        loadSuggestedPeople()
        loadTrendingPlaces()
    }

    fun loadMyFollowRelations() {
        val uid = currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val myFollowingsResult = profileRepository.getFollowing(uid).getOrDefault(emptyList())
                val myFollowersResult = profileRepository.getFollowers(uid).getOrDefault(emptyList())
                val myFollowings = myFollowingsResult.map { it.uid }.toSet()
                val myFollowers = myFollowersResult.map { it.uid }.toSet()
                _uiState.update {
                    it.copy(
                        myFollowingIds = myFollowings,
                        myFollowerIds = myFollowers
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun loadRecentSearches() {
        val uid = currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = profileRepository.getRecentSearches(uid)
            val list = result.getOrDefault(emptyList())
            _uiState.update { it.copy(recentSearches = list) }
        }
    }

    fun loadSuggestedPeople() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = profileRepository.searchProfiles("", limit = 10)
            val users = result.getOrDefault(emptyList())
                .filter { it.uid != currentUserId }
            _uiState.update { it.copy(suggestedPeople = users) }
        }
    }
    fun loadTrendingPlaces() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val posts = feedRepository.getFeedPostsBatch(100, null)
                val postLocations = posts.mapNotNull { it.location }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(5)

                val trending = mutableListOf<PlaceUiModel>()
                val fallbackPlaces = getSuggestedPlaces()

                postLocations.forEachIndexed { index, entry ->
                    val locationName = entry.key
                    val existingFallback = fallbackPlaces.find { it.name.lowercase() == locationName.lowercase() }
                    if (existingFallback != null) {
                        trending.add(existingFallback.copy(id = "real_$index"))
                    } else {
                        val matchingPost = posts.find { it.location == locationName }
                        trending.add(
                            PlaceUiModel(
                                id = "real_$index",
                                name = locationName,
                                category = "Địa điểm thịnh hành",
                                rating = "4.8",
                                distance = "1.5 km",
                                priceRange = "$$",
                                image = matchingPost?.imageUrls?.firstOrNull() ?: fallbackPlaces.first().image
                            )
                        )
                    }
                }

                if (trending.size < 3) {
                    fallbackPlaces.forEach { fallback ->
                        if (trending.none { it.name.lowercase() == fallback.name.lowercase() }) {
                            trending.add(fallback)
                        }
                    }
                }

                _uiState.update { it.copy(trendingPlaces = trending.take(5)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(trendingPlaces = getSuggestedPlaces().take(5)) }
            }
        }
    }

    fun deleteRecentSearch(query: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = _uiState.value.recentSearches.filterNot { it == query }
            _uiState.update { it.copy(recentSearches = currentList) }

            val searchId = query.trim().lowercase()
            firestore.collection("users")
                .document(uid)
                .collection("recentSearches")
                .document(searchId)
                .delete()
        }
    }

    fun clearAllRecentSearches() {
        val uid = currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(recentSearches = emptyList()) }
            profileRepository.clearRecentSearches(uid)
        }
    }


    private fun buildPlaceResults(query: String, posts: List<Post>): List<PlaceUiModel> {
        val fallbackPlaces = getSuggestedPlaces()
        val fallbackImage = fallbackPlaces.firstOrNull()?.image.orEmpty()

        val placesFromPosts = posts
            .mapNotNull { post ->
                val location = post.location?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                location to post
            }
            .groupBy { (location, _) -> location.lowercase() }
            .entries
            .mapIndexed { index, entry ->
                val locationName = entry.value.first().first
                val relatedPosts = entry.value.map { it.second }
                val matchingFallback = fallbackPlaces.find { it.name.equals(locationName, ignoreCase = true) }

                matchingFallback?.copy(
                    id = "post_location_$index",
                    category = "Có ${relatedPosts.size} bài viết liên quan",
                    image = relatedPosts.firstNotNullOfOrNull { it.imageUrls.firstOrNull() } ?: matchingFallback.image
                ) ?: PlaceUiModel(
                    id = "post_location_$index",
                    name = locationName,
                    category = "Có ${relatedPosts.size} bài viết liên quan",
                    rating = "4.8",
                    distance = "Từ bảng tin",
                    priceRange = "DineSplit",
                    image = relatedPosts.firstNotNullOfOrNull { it.imageUrls.firstOrNull() } ?: fallbackImage
                )
            }

        val fallbackMatches = fallbackPlaces.filter { place ->
            place.name.lowercase().contains(query) || place.category.lowercase().contains(query)
        }

        return (placesFromPosts + fallbackMatches)
            .distinctBy { it.name.lowercase() }
            .take(12)
    }

    private fun getSuggestedPlaces(): List<PlaceUiModel> {
        return listOf(
            PlaceUiModel("sp_1", "The Rustic Spoons", "Artisanal Italian", "4.8", "1.2 km", "$$$", "https://lh3.googleusercontent.com/aida-public/AB6AXuAnKvQKCCduaFahO55imH8Cl_EDOrKzD3axmpQl65HQdBlT-AvIAYxjxe-Iq2cLOXN_QBN51DrnEjhtaPAWPe2pT7QCSXolQ6eQIQfn0KozsJ7NprB-f8mJStrrAmYHt6Ifz9821HotOldGO8chnQ9MDmdEPqu4pFmpzKUh1zLllrrHhJIBDC0hTE8erOZBXMsqYcqXhnycvovS241S6TCDAym__w04HbAcz2mnktnxKVfuWgKkmgy2lSRvRh6lHSLGA17PyB6W-Q"),
            PlaceUiModel("sp_2", "Urban Greens", "Healthy Bowls", "4.6", "0.8 km", "$$", "https://lh3.googleusercontent.com/aida-public/AB6AXuCkxScXIbLdu6IiOy23I0nbl2-tb8Zhwv5q6IOHlBqhaI2piPVrmIp0iMOuGCPZfmQDg5OkvifWKNUMCEKZ0h0a09Qa-OdIzFKMjHGqjbOv_ri4hEO1W_ofZ6RZxhjH2ey-gZ8jBqTOc1ErG-cGKZPNsxALDyFAM6xHYP_SYCrz-7gdSTyMWUv50ARCoL_Bvlfg9uwEHrWaKpCgHqR7QGFApntACPfJMHNoW1Q1PGfbIZyxPiQs2p1ksTVn6_v_uIGlBbDuhvVBpQ"),
            PlaceUiModel("sp_3", "Pizza 4P's Tràng Tiền", "Pizza & Italian", "4.9", "0.5 km", "$$$", "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500&auto=format&fit=crop&q=60"),
            PlaceUiModel("sp_4", "Phở Thìn Lò Đúc", "Món Việt Truyền Thống", "4.7", "2.1 km", "$", "https://images.unsplash.com/photo-1582878826629-29b7ad8cd305?w=500&auto=format&fit=crop&q=60"),
            PlaceUiModel("sp_5", "Bún Chả Hương Liên", "Bún Chả Hà Nội", "4.8", "2.4 km", "$$", "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=60")
        )
    }
}
