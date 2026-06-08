package com.example.dinesplit.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.core.ui.SmallButton
import com.example.dinesplit.presentation.feed.search.PlaceUiModel
import com.example.dinesplit.presentation.feed.search.SearchFilter
import com.example.dinesplit.presentation.feed.search.SearchViewModel
import com.example.dinesplit.presentation.feed.search.components.*

/**
 * Màn hình Tìm kiếm (Search Screen)
 * Cho phép người dùng tìm kiếm bạn bè, bài viết ăn uống, hoặc địa điểm ẩm thực.
 * Cung cấp tính năng xem gợi ý bạn bè, địa điểm xu hướng, lịch sử tìm kiếm gần đây
 * và mở địa điểm trên ứng dụng bản đồ (Google Maps).
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenPostDetail: (String) -> Unit, // Đã kết nối luồng click mở chi tiết bài viết
    onOpenUserProfile: (String) -> Unit, // Đã kết nối luồng click mở trang cá nhân người khác
    viewModel: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    // Lắng nghe trạng thái UI từ ViewModel
    val uiState by viewModel.uiState.collectAsState()
    // Lưu trữ địa điểm đang được chọn để xem chi tiết
    var selectedPlaceForDetail by remember { mutableStateOf<PlaceUiModel?>(null) }

    // Tải các quan hệ follow/follower để hiển thị trạng thái nút Theo dõi chính xác
    LaunchedEffect(Unit) {
        viewModel.loadMyFollowRelations()
    }

    // Dialog thông tin chi tiết địa điểm và tuỳ chọn định hướng Maps
    if (selectedPlaceForDetail != null) {
        val place = selectedPlaceForDetail!!
        AlertDialog(
            onDismissRequest = { selectedPlaceForDetail = null },
            confirmButton = {
                SmallButton(
                    text = "Xem trên Google Maps",
                    onClick = {
                        // Thử mở ứng dụng Google Maps trực tiếp bằng geo-URI
                        val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(place.name)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            // Nếu thiết bị không cài Google Maps, mở qua liên kết web
                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(place.name)}")
                            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                            context.startActivity(webIntent)
                        }
                        selectedPlaceForDetail = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { selectedPlaceForDetail = null }) {
                    Text("Đóng")
                }
            },
            title = {
                Text(
                    text = place.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (place.image.isNotEmpty()) {
                        AsyncImage(
                            model = place.image,
                            contentDescription = place.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = "Danh mục: ${place.category}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = place.rating, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "Giá: ${place.priceRange}")
                        Text(text = "Khoảng cách: ${place.distance}")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            // Thanh công cụ tìm kiếm trên cùng chứa ô nhập liệu và nút quay lại
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
            
            // Xử lý các trạng thái tải dữ liệu, lỗi và không có kết quả
            when {
                // Trạng thái đang tải dữ liệu (Loading)
                state.isLoading -> {
                    LoadingBlock(
                        message = "Đang tìm kiếm...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.spaceLg)
                    )
                }

                // Trạng thái lỗi (Ví dụ lỗi kết nối mạng)
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

                // Trạng thái không có kết quả tìm kiếm nào khớp
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
                        contentPadding = PaddingValues(bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
                    ) {
                        // CHẾ ĐỘ KHÁM PHÁ (Khi chưa nhập từ khóa tìm kiếm)
                        if (state.isExploreMode) {
                            
                            // 1. Danh sách từ khóa tìm kiếm gần đây (Lịch sử tìm kiếm)
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

                            // 2. Danh sách gợi ý bạn bè kết nối (Những người dùng khác gợi ý theo dõi)
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
                                    val isFollowing = uiState.myFollowingIds.contains(user.uid)
                                    val isFollower = uiState.myFollowerIds.contains(user.uid)
                                    val isMe = user.uid == viewModel.currentUserId
                                    Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                        SearchPersonCard(
                                            user = user,
                                            isFollowing = isFollowing,
                                            isFollower = isFollower,
                                            isMe = isMe,
                                            onClick = { onOpenUserProfile(user.uid) }
                                        )
                                    }
                                }
                            }

                            // 3. Danh sách địa điểm ăn uống nổi bật / thịnh hành
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
                                            onClick = { selectedPlaceForDetail = place }
                                        )
                                    }
                                }
                            }

                        } else {
                            // CHẾ ĐỘ HIỂN THỊ KẾT QUẢ TÌM KIẾM (Khi đã nhập từ khóa)
                            
                            // Thanh bộ lọc kết quả tìm kiếm (Tất cả, Bài viết, Bạn bè, Địa điểm)
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

                            // Phân loại kết quả tìm kiếm theo bộ lọc đang được chọn
                            when (state.selectedFilter) {
                                SearchFilter.All -> {
                                    // 1. Phân vùng xem trước: Người dùng tương thích (Lấy tối đa 3 kết quả)
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
                                            val isFollowing = uiState.myFollowingIds.contains(user.uid)
                                            val isFollower = uiState.myFollowerIds.contains(user.uid)
                                            val isMe = user.uid == viewModel.currentUserId
                                            Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                                SearchPersonCard(
                                                    user = user,
                                                    isFollowing = isFollowing,
                                                    isFollower = isFollower,
                                                    isMe = isMe,
                                                    onClick = { onOpenUserProfile(user.uid) }
                                                )
                                            }
                                        }
                                    }

                                    // 2. Phân vùng xem trước: Bài viết ăn uống (Lấy tối đa 5 kết quả)
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

                                    // 3. Phân vùng xem trước: Địa điểm ẩm thực (Lấy tối đa 3 kết quả)
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
                                                    onClick = { selectedPlaceForDetail = place }
                                                )
                                            }
                                        }
                                    }
                                }

                                SearchFilter.Posts -> {
                                    // Lọc duy nhất danh sách Bài viết
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
                                    // Lọc duy nhất danh sách Người dùng / Bạn bè
                                    items(
                                        items = state.peopleResults,
                                        key = { "result_person_only_${it.uid}" }
                                    ) { user ->
                                        val isFollowing = uiState.myFollowingIds.contains(user.uid)
                                        val isFollower = uiState.myFollowerIds.contains(user.uid)
                                        val isMe = user.uid == viewModel.currentUserId
                                        Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                            SearchPersonCard(
                                                user = user,
                                                isFollowing = isFollowing,
                                                isFollower = isFollower,
                                                isMe = isMe,
                                                onClick = { onOpenUserProfile(user.uid) }
                                            )
                                        }
                                    }
                                }

                                SearchFilter.Places -> {
                                    // Lọc duy nhất danh sách Địa điểm
                                    items(
                                        items = state.placeResults,
                                        key = { "result_place_only_${it.id}" }
                                    ) { place ->
                                        Box(modifier = Modifier.padding(horizontal = AppDimens.spaceLg)) {
                                            SearchPlaceCard(
                                                place = place,
                                                onClick = { selectedPlaceForDetail = place }
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