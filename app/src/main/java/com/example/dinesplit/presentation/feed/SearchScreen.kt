package com.example.dinesplit.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.presentation.feed.search.SearchFilter
import com.example.dinesplit.presentation.feed.search.SearchViewModel
import com.example.dinesplit.presentation.feed.search.components.*

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenPostDetail: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
    viewModel: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            SearchTopBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onBack = onBack,
                onSearchAction = viewModel::executeSearch
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingBlock(
                        message = "Đang tìm kiếm...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.spaceLg)
                    )
                }

                uiState.errorMessage != null -> {
                    ErrorStateBlock(
                        title = "Không thể tìm kiếm",
                        subtitle = uiState.errorMessage!!,
                        retryText = "Thử lại",
                        onRetryClick = viewModel::executeSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.spaceLg)
                    )
                }

                uiState.hasNoResult -> {
                    EmptyStateBlock(
                        title = "Không tìm thấy kết quả",
                        subtitle = "Thử tìm kiếm với từ khóa khác như món ăn, địa điểm hoặc tên người dùng.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.spaceLg)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
                    ) {
                        if (uiState.isExploreMode) {
                            // EXPLORE MODE
                            
                            // Recent searches
                            if (uiState.recentSearches.isNotEmpty()) {
                                item {
                                    Column(modifier = Modifier.padding(top = AppDimens.spaceMd)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Tìm kiếm gần đây",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "Xóa tất cả",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable { viewModel.clearAllRecentSearches() }
                                            )
                                        }

                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = AppDimens.spaceLg),
                                            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                                        ) {
                                            items(
                                                items = uiState.recentSearches,
                                                key = { "recent_$it" }
                                            ) { search ->
                                                RecentSearchChip(
                                                    text = search,
                                                    onClick = { viewModel.onQueryChange(search) },
                                                    onDeleteClick = { viewModel.deleteRecentSearch(search) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Discover People (Gợi ý kết nối bạn bè)
                            if (uiState.suggestedPeople.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Khám phá bạn bè",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
                                    )
                                }

                                items(
                                    items = uiState.suggestedPeople,
                                    key = { "suggested_${it.uid}" }
                                ) { user ->
                                    Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                        SearchPersonCard(
                                            user = user,
                                            onClick = { onOpenUserProfile(user.uid) }
                                        )
                                    }
                                }
                            }

                            // Trending Places (Địa điểm ăn uống xu hướng)
                            if (uiState.trendingPlaces.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Địa điểm nổi bật",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
                                    )
                                }

                                items(
                                    items = uiState.trendingPlaces,
                                    key = { "trending_${it.id}" }
                                ) { place ->
                                    Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                        SearchPlaceCard(
                                            place = place,
                                            onClick = { }
                                        )
                                    }
                                }
                            }

                        } else {
                            // RESULT MODE
                            
                            // Category chips selection
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
                                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                                ) {
                                    items(SearchFilter.values()) { filter ->
                                        FilterChip(
                                            selected = uiState.selectedFilter == filter,
                                            onClick = { viewModel.onFilterChange(filter) },
                                            label = { Text(filter.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            }

                            when (uiState.selectedFilter) {
                                SearchFilter.All -> {
                                    // 1. Users category preview
                                    if (uiState.peopleResults.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Người dùng",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm)
                                            )
                                        }

                                        items(
                                            items = uiState.peopleResults.take(3),
                                            key = { "result_person_all_${it.uid}" }
                                        ) { user ->
                                            Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                                SearchPersonCard(
                                                    user = user,
                                                    onClick = { onOpenUserProfile(user.uid) }
                                                )
                                            }
                                        }
                                    }

                                    // 2. Posts category preview
                                    if (uiState.postResults.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Bài viết",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm)
                                            )
                                        }

                                        items(
                                            items = uiState.postResults.take(5),
                                            key = { "result_post_all_${it.id}" }
                                        ) { post ->
                                            Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                                SearchPostCard(
                                                    post = post,
                                                    onClick = { onOpenPostDetail(post.id) },
                                                    onAuthorClick = { onOpenUserProfile(post.authorUid) }
                                                )
                                            }
                                        }
                                    }

                                    // 3. Places category preview
                                    if (uiState.placeResults.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Địa điểm",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm)
                                            )
                                        }

                                        items(
                                            items = uiState.placeResults.take(3),
                                            key = { "result_place_all_${it.id}" }
                                        ) { place ->
                                            Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                                SearchPlaceCard(
                                                    place = place,
                                                    onClick = { }
                                                )
                                            }
                                        }
                                    }
                                }

                                SearchFilter.Posts -> {
                                    items(
                                        items = uiState.postResults,
                                        key = { "result_post_only_${it.id}" }
                                    ) { post ->
                                        Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                            SearchPostCard(
                                                post = post,
                                                onClick = { onOpenPostDetail(post.id) },
                                                onAuthorClick = { onOpenUserProfile(post.authorUid) }
                                            )
                                        }
                                    }
                                }

                                SearchFilter.People -> {
                                    items(
                                        items = uiState.peopleResults,
                                        key = { "result_person_only_${it.uid}" }
                                    ) { user ->
                                        Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                            SearchPersonCard(
                                                user = user,
                                                onClick = { onOpenUserProfile(user.uid) }
                                            )
                                        }
                                    }
                                }

                                SearchFilter.Places -> {
                                    items(
                                        items = uiState.placeResults,
                                        key = { "result_place_only_${it.id}" }
                                    ) { place ->
                                        Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                            SearchPlaceCard(
                                                place = place,
                                                onClick = { }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
