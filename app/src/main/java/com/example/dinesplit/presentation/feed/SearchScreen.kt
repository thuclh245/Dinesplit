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
    onOpenPostDetail: (String) -> Unit, // Đã kết nối luồng click mở chi tiết bài viết
    onOpenUserProfile: (String) -> Unit, // Đã kết nối luồng click mở trang cá nhân người khác
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
            val state = uiState
            
            when {
                state.isLoading -> {
                    LoadingBlock(
                        message = "Đang tìm kiếm...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.spaceLg)
                    )
                }

                // TRẠNG THÁI LỖI MẠNG (Đồng bộ xử lý bẫy lỗi Tuần 5)
                state.errorMessage != null -> {
                    ErrorStateBlock(
                        title = "Không thể tìm kiếm",
                        subtitle = state.errorMessage!!,
                        retryText = "Thử lại",
                        onRetryClick = viewModel::executeSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.spaceLg)
                    )
                }

                state.hasNoResult -> {
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
                        if (state.isExploreMode) {
                            
                            // Danh sách từ khóa tìm kiếm gần đây
                            if (state.recentSearches.isNotEmpty()) {
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
                                                items = state.recentSearches,
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

                            // Khám phá gợi ý bạn bè kết nối
                            if (state.suggestedPeople.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Khám phá bạn bè",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
                                    )
                                }

                                items(
                                    items = state.suggestedPeople,
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

                            // Địa điểm ăn uống nổi bật xu hướng
                            if (state.trendingPlaces.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Địa điểm nổi bật",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
                                    )
                                }

                                items(
                                    items = state.trendingPlaces,
                                    key = { "trending_${it.id}" }
                                ) { place ->
                                    Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                        SearchPlaceCard(
                                            place = place,
                                            onClick = { /* Xử lý nếu mở chi tiết địa điểm */ }
                                        )
                                    }
                                }
                            }

                        } else {
                            
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
                                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                                ) {
                                    // TUẦN 6 OPTIMIZATION: Thay thế .values() bằng .entries để tránh sao chép cấp phát mảng thừa trong bộ nhớ
                                    items(SearchFilter.entries) { filter ->
                                        FilterChip(
                                            selected = state.selectedFilter == filter,
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

                            when (state.selectedFilter) {
                                SearchFilter.All -> {
                                    // 1. Phân vùng xem trước: Người dùng tương thích
                                    if (state.peopleResults.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Người dùng",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm)
                                            )
                                        }
                                        items(
                                            items = state.peopleResults.take(3),
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

                                    if (state.postResults.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Bài viết",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm)
                                            )
                                        }
                                        items(
                                            items = state.postResults.take(5),
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

                                    if (state.placeResults.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Địa điểm",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm)
                                            )
                                        }
                                        items(
                                            items = state.placeResults.take(3),
                                            key = { "result_place_all_${it.id}" }
                                        ) { place ->
                                            Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                                SearchPlaceCard(
                                                    place = place,
                                                    onClick = { /* Xử lý mở vị trí map */ }
                                                )
                                            }
                                        }
                                    }
                                }

                                SearchFilter.Posts -> {
                                    items(
                                        items = state.postResults,
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
                                        items = state.peopleResults,
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
                                        items = state.placeResults,
                                        key = { "result_place_only_${it.id}" }
                                    ) { place ->
                                        Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                            SearchPlaceCard(
                                                place = place,
                                                onClick = { /* Xử lý click */ }
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