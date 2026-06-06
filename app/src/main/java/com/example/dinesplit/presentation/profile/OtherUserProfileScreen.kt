package com.example.dinesplit.presentation.profile

import android.app.Application
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.DineGridImage
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.core.ui.SmallButton
import com.example.dinesplit.domain.model.LinkedBillSummary
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.UserProfile
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OtherUserProfileScreen(
    userName: String, // represents the target user's UID (passed from navigation)
    onBack: () -> Unit = {},
    onNavigateToFollowList: (String, Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val vm: OtherUserProfileViewModel = viewModel(
        factory = OtherUserProfileViewModel.Factory(application, userName)
    )
    val uiState by vm.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    AppScaffold(
        title = uiState.profile?.username?.let { "@$it" } ?: "Hồ sơ",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
        },
    ) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingBlock(message = "Đang tải hồ sơ...")
                }
            }
            uiState.errorMessage != null && uiState.profile == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorStateBlock(
                        title = "Lỗi tải hồ sơ",
                        subtitle = uiState.errorMessage ?: "Không tìm thấy người dùng này.",
                        retryText = "Quay lại",
                        onRetryClick = onBack
                    )
                }
            }
            else -> {
                val profile = uiState.profile
                if (profile != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        OtherProfileHeader(
                            profile = profile,
                            isFollowing = uiState.isFollowing,
                            isFollowedByOther = uiState.isFollowedByOther,
                            isFollowActionBusy = uiState.isFollowActionBusy,
                            postsCount = uiState.posts.size,
                            onToggleFollow = vm::toggleFollow,
                            onFollowingClick = { onNavigateToFollowList(profile.uid, 0) },
                            onFollowersClick = { onNavigateToFollowList(profile.uid, 1) }
                        )

                        val isMutualFriend = uiState.isFollowing && uiState.isFollowedByOther
                        val showPrivateLock = !profile.isPublic && !isMutualFriend

                        if (showPrivateLock) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Tài khoản riêng tư",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Tài khoản này là riêng tư",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Hãy kết bạn (theo dõi lẫn nhau) để xem các bài viết và hóa đơn chung.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            OtherProfileTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

                            when (selectedTab) {
                                0 -> {
                                    val postsWithImages = uiState.posts.filter { it.imageUrls.isNotEmpty() }
                                    if (postsWithImages.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoLibrary,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.outline.copy(0.6f),
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Text(
                                                    text = "Chưa có bài đăng nào",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    } else {
                                        OtherPhotoGrid(posts = uiState.posts, onPostClick = { /* Can navigate to post detail if needed */ })
                                    }
                                }
                                1 -> {
                                    OtherTaggedBillsList(bills = uiState.taggedBills, onBillClick = { _, _ -> })
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OtherProfileHeader(
    profile: UserProfile,
    isFollowing: Boolean,
    isFollowedByOther: Boolean,
    isFollowActionBusy: Boolean,
    postsCount: Int,
    onToggleFollow: () -> Unit,
    onFollowingClick: () -> Unit,
    onFollowersClick: () -> Unit,
) {
    val resolvedBio = profile.bio.ifBlank { "Người dùng này chưa cập nhật tiểu sử." }

    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Avatar with Gradient Ring
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary,
                            ),
                        ),
                        CircleShape,
                    )
                    .padding(3.dp),
            ) {
                DineAvatarImage(
                    imageUrl = profile.avatarUrl,
                    name = profile.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
                    size = 94.dp,
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (profile.username.isNotBlank()) "@${profile.username}" else "@user",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallButton(
                        text = when {
                            isFollowing && isFollowedByOther -> "Bạn bè"
                            isFollowing -> "Đang theo dõi"
                            isFollowedByOther -> "Theo dõi lại"
                            else -> "Theo dõi"
                        },
                        onClick = onToggleFollow,
                        enabled = !isFollowActionBusy,
                        isLoading = isFollowActionBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .semantics {
                                contentDescription = if (isFollowing) "Bỏ theo dõi người dùng" else "Theo dõi người dùng"
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) {
                                MaterialTheme.colorScheme.surfaceContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            contentColor = if (isFollowing) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            OtherStatItem(label = "Bài viết", value = postsCount.toString())
            OtherStatItem(
                label = "Theo dõi",
                value = profile.followersCount.toString(),
                modifier = Modifier.clickable { onFollowersClick() }
            )
            OtherStatItem(
                label = "Đang theo",
                value = profile.followingCount.toString(),
                modifier = Modifier.clickable { onFollowingClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = resolvedBio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OtherStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun OtherProfileTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val tabs = listOf(
        TabInfo("Bài viết", Icons.Default.GridView),
        TabInfo("Hóa đơn chung", Icons.Default.ReceiptLong),
    )
    val primaryColor = MaterialTheme.colorScheme.primary

    Column {
        Row(modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight)) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onTabSelected(index) }
                        )
                        .semantics(mergeDescendants = true) {}
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

@Composable
private fun OtherPhotoGrid(
    posts: List<Post>,
    onPostClick: (String) -> Unit,
) {
    val postsWithImages = posts.filter { it.imageUrls.isNotEmpty() }
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        val rows = postsWithImages.chunked(3)
        rows.forEach { rowPosts ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowPosts.forEach { post ->
                    val url = post.imageUrls.first()
                    DineGridImage(
                        imageUrl = url,
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onPostClick(post.id) },
                    )
                }
                repeat(3 - rowPosts.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OtherTaggedBillsList(
    bills: List<LinkedBillSummary>,
    onBillClick: (String, String) -> Unit
) {
    if (bills.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(0.6f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Không có hoạt động chia tiền chung",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            bills.forEach { summary ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBillClick(summary.groupId, summary.billId) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = summary.billName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Surface(
                                color = if (summary.isSettled) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                },
                                shape = CircleShape,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (summary.isSettled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    text = if (summary.isSettled) "ĐÃ THANH TOÁN" else "CHƯA THANH TOÁN",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (summary.isSettled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Tổng hóa đơn",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = otherFormatMoney(summary.totalAmount),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (summary.isIPayer) "Họ đã trả trước" else "Phần của họ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = otherFormatMoney(summary.myShare),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (summary.isSettled || summary.isMyPaid || summary.isIPayer) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun otherFormatMoney(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return formatter.format(amount)
}
