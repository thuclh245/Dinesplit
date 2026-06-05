package com.example.dinesplit.presentation.profile

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.SearchTextField
import com.example.dinesplit.core.ui.SmallButton
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.domain.model.UserProfile

@Composable
fun FollowListScreen(
    userId: String,
    initialTab: Int = 0, // 0: Following, 1: Followers, 2: Friends
    onBack: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val vm: FollowListViewModel = viewModel(
        factory = FollowListViewModel.Factory(application, userId)
    )
    val uiState by vm.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(initialTab) }

    val loggedInUserId = FirebaseProviders.auth.currentUser?.uid

    AppScaffold(
        title = uiState.inspectedUser?.displayName ?: "Hồ sơ",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
        }
    ) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingBlock(message = "Đang tải danh sách...")
                }
            }
            uiState.errorMessage != null && uiState.inspectedUser == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorStateBlock(
                        title = "Lỗi tải danh sách",
                        subtitle = uiState.errorMessage ?: "Không tìm thấy thông tin người dùng này.",
                        retryText = "Quay lại",
                        onRetryClick = onBack
                    )
                }
            }
            else -> {
                val user = uiState.inspectedUser
                if (user != null) {
                    val isMe = userId == loggedInUserId
                    val followingCount = if (isMe) uiState.myFollowingIds.size else uiState.following.size
                    val followersCount = if (isMe) uiState.myFollowerIds.size else uiState.followers.size
                    val friendsCount = if (isMe) (uiState.myFollowingIds intersect uiState.myFollowerIds).size else uiState.friends.size

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Tab Row
                        FollowTabs(
                            selectedTab = selectedTab,
                            followingCount = followingCount,
                            followersCount = followersCount,
                            friendsCount = friendsCount,
                            onTabSelected = { selectedTab = it }
                        )

                        // Search Bar
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            SearchTextField(
                                value = uiState.searchQuery,
                                onValueChange = vm::onSearchQueryChange,
                                label = "",
                                placeholder = "Tìm kiếm theo tên hoặc username...",
                                onClearClick = { vm.onSearchQueryChange("") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Users list inside the active tab
                        val activeList = when (selectedTab) {
                            0 -> uiState.following
                            1 -> uiState.followers
                            else -> uiState.friends
                        }

                        val filteredList = activeList.filter {
                            it.displayName.contains(uiState.searchQuery, ignoreCase = true) ||
                                    it.username.contains(uiState.searchQuery, ignoreCase = true)
                        }

                        if (filteredList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (uiState.searchQuery.isEmpty()) {
                                        "Danh sách này trống."
                                    } else {
                                        "Không tìm thấy người dùng phù hợp."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 32.dp)
                            ) {
                                items(filteredList, key = { it.uid }) { targetUser ->
                                    val isMe = targetUser.uid == loggedInUserId
                                    val isFollowing = uiState.myFollowingIds.contains(targetUser.uid)
                                    val isFollower = uiState.myFollowerIds.contains(targetUser.uid)
                                    val isActionBusy = uiState.followActionBusyUserIds.contains(targetUser.uid)

                                    FollowUserRow(
                                        user = targetUser,
                                        isMe = isMe,
                                        isFollowing = isFollowing,
                                        isFollower = isFollower,
                                        isActionBusy = isActionBusy,
                                        onUserClick = { onUserClick(targetUser.uid) },
                                        onFollowActionClick = { vm.toggleFollowUserInList(targetUser.uid) }
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

@Composable
private fun FollowTabs(
    selectedTab: Int,
    followingCount: Int,
    followersCount: Int,
    friendsCount: Int,
    onTabSelected: (Int) -> Unit,
) {
    val tabs = listOf(
        "Đang theo dõi $followingCount",
        "Người theo dõi $followersCount",
        "Bạn bè $friendsCount"
    )
    val primaryColor = MaterialTheme.colorScheme.primary

    Column {
        Row(modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight)) {
            tabs.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(index) }
                        .drawBehind {
                            if (isSelected) {
                                drawLine(
                                    color = primaryColor,
                                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                                    strokeWidth = 2.dp.toPx(),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

@Composable
private fun FollowUserRow(
    user: UserProfile,
    isMe: Boolean,
    isFollowing: Boolean,
    isFollower: Boolean,
    isActionBusy: Boolean,
    onUserClick: () -> Unit,
    onFollowActionClick: () -> Unit,
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DineAvatarImage(imageUrl = user.avatarUrl, name = user.displayName, size = 48.dp)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (user.username.isNotBlank()) "@${user.username}" else "@user",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isMe) {
                val buttonText = when {
                    isFollowing && isFollower -> "Bạn bè"
                    isFollowing -> "Đang theo dõi"
                    isFollower -> "Theo dõi lại"
                    else -> "Theo dõi"
                }

                val isOutline = isFollowing // "Bạn bè" or "Đang theo dõi" are outline/secondary buttons

                SmallButton(
                    text = buttonText,
                    onClick = onFollowActionClick,
                    enabled = !isActionBusy,
                    isLoading = isActionBusy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOutline) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        contentColor = if (isOutline) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .widthIn(min = 100.dp)
                )
            }

            // Three-dots action menu (visual completeness matching TikTok layout)
            IconButton(onClick = { /* Option menu can be shown if needed */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Tùy chọn",
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}
