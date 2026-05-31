package com.example.dinesplit.presentation.feed.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val profileRepository = AppContainer.profileRepository(application)
    private val feedRepository = AppContainer.feedRepository()
    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var searchJob: Job? = null

    init {
        // Quan sát phiên đăng nhập của người dùng để lấy lịch sử tìm kiếm và các đề xuất
        viewModelScope.launch {
            observeSessionUseCase().collect { session ->
                currentUserId = session?.uid
                loadExploreData()
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)

        // Hủy job tìm kiếm trước đó nếu đang nhập liên tiếp (Debounce)
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(
                postResults = emptyList(),
                peopleResults = emptyList(),
                placeResults = emptyList(),
                hasSearched = false,
                isLoading = false,
                errorMessage = null
            )
        } else {
            searchJob = viewModelScope.launch {
                delay(350)
                executeSearch()
            }
        }
    }

    fun onFilterChange(newFilter: SearchFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = newFilter)
        if (_uiState.value.query.isNotBlank()) {
            executeSearch()
        }
    }

    fun executeSearch() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // 1. Tìm người dùng
                val usersResult = profileRepository.searchProfiles(query)
                val users = usersResult.getOrDefault(emptyList())

                // 2. Tìm bài viết
                val posts = feedRepository.searchPosts(query)

                // 3. Tìm địa điểm (từ danh sách tuyển chọn + khớp từ khóa)
                val places = getSuggestedPlaces().filter { place ->
                    place.name.lowercase().contains(query.lowercase()) ||
                        place.category.lowercase().contains(query.lowercase())
                }

                _uiState.value = _uiState.value.copy(
                    postResults = posts,
                    peopleResults = users,
                    placeResults = places,
                    isLoading = false,
                    hasSearched = true
                )

                // Lưu lại lịch sử tìm kiếm
                currentUserId?.let { uid ->
                    profileRepository.saveRecentSearch(uid, query)
                    loadRecentSearches()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Đã xảy ra lỗi khi tìm kiếm"
                )
            }
        }
    }

    fun loadExploreData() {
        loadRecentSearches()
        loadSuggestedPeople()
        loadTrendingPlaces()
    }

    fun loadRecentSearches() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val result = profileRepository.getRecentSearches(uid)
            val list = result.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(recentSearches = list)
        }
    }

    fun loadSuggestedPeople() {
        viewModelScope.launch {
            val result = profileRepository.searchProfiles("", limit = 10)
            val users = result.getOrDefault(emptyList())
                .filter { it.uid != currentUserId } // Không gợi ý chính mình
            _uiState.value = _uiState.value.copy(suggestedPeople = users)
        }
    }

    fun loadTrendingPlaces() {
        viewModelScope.launch {
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

                // Trích xuất các vị trí thật từ bài đăng
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

                // Nếu ít hơn 3 địa điểm, bù thêm bằng fallback chất lượng cao
                if (trending.size < 3) {
                    fallbackPlaces.forEach { fallback ->
                        if (trending.none { it.name.lowercase() == fallback.name.lowercase() }) {
                            trending.add(fallback)
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(trendingPlaces = trending.take(5))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(trendingPlaces = getSuggestedPlaces().take(5))
            }
        }
    }

    fun deleteRecentSearch(query: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val currentList = _uiState.value.recentSearches.filterNot { it == query }
            _uiState.value = _uiState.value.copy(recentSearches = currentList)

            val searchId = query.trim().lowercase()
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("users")
                .document(uid)
                .collection("recentSearches")
                .document(searchId)
                .delete()
        }
    }

    fun clearAllRecentSearches() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(recentSearches = emptyList())
            profileRepository.clearRecentSearches(uid)
        }
    }

    private fun getSuggestedPlaces(): List<PlaceUiModel> {
        return listOf(
            PlaceUiModel(
                id = "sp_1",
                name = "The Rustic Spoons",
                category = "Artisanal Italian",
                rating = "4.8",
                distance = "1.2 km",
                priceRange = "$$$",
                image = "https://lh3.googleusercontent.com/aida-public/AB6AXuAnKvQKCCduaFahO55imH8Cl_EDOrKzD3axmpQl65HQdBlT-AvIAYxjxe-Iq2cLOXN_QBN51DrnEjhtaPAWPe2pT7QCSXolQ6eQIQfn0KozsJ7NprB-f8mJStrrAmYHt6Ifz9821HotOldGO8chnQ9MDmdEPqu4pFmpzKUh1zLllrrHhJIBDC0hTE8erOZBXMsqYcqXhnycvovS241S6TCDAym__w04HbAcz2mnktnxKVfuWgKkmgy2lSRvRh6lHSLGA17PyB6W-Q"
            ),
            PlaceUiModel(
                id = "sp_2",
                name = "Urban Greens",
                category = "Healthy Bowls",
                rating = "4.6",
                distance = "0.8 km",
                priceRange = "$$",
                image = "https://lh3.googleusercontent.com/aida-public/AB6AXuCkxScXIbLdu6IiOy23I0nbl2-tb8Zhwv5q6IOHlBqhaI2piPVrmIp0iMOuGCPZfmQDg5OkvifWKNUMCEKZ0h0a09Qa-OdIzFKMjHGqjbOv_ri4hEO1W_ofZ6RZxhjH2ey-gZ8jBqTOc1ErG-cGKZPNsxALDyFAM6xHYP_SYCrz-7gdSTyMWUv50ARCoL_Bvlfg9uwEHrWaKpCgHqR7QGFApntACPfJMHNoW1Q1PGfbIZyxPiQs2p1ksTVn6_v_uIGlBbDuhvVBpQ"
            ),
            PlaceUiModel(
                id = "sp_3",
                name = "Pizza 4P's Tràng Tiền",
                category = "Pizza & Italian",
                rating = "4.9",
                distance = "0.5 km",
                priceRange = "$$$",
                image = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500&auto=format&fit=crop&q=60"
            ),
            PlaceUiModel(
                id = "sp_4",
                name = "Phở Thìn Lò Đúc",
                category = "Món Việt Truyền Thống",
                rating = "4.7",
                distance = "2.1 km",
                priceRange = "$",
                image = "https://images.unsplash.com/photo-1582878826629-29b7ad8cd305?w=500&auto=format&fit=crop&q=60"
            ),
            PlaceUiModel(
                id = "sp_5",
                name = "Bún Chả Hương Liên",
                category = "Bún Chả Hà Nội",
                rating = "4.8",
                distance = "2.4 km",
                priceRange = "$$",
                image = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=60"
            )
        )
    }
}
