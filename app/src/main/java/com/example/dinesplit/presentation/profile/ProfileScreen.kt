package com.example.dinesplit.presentation.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.DineGridImage
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton
import com.example.dinesplit.domain.model.LinkedBillSummary
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.ui.theme.DineSplitTheme
import java.text.NumberFormat
import java.util.Locale


@Composable
fun ProfileScreen(
    userAvatarUrl: String?,
    userName: String,
    userHandle: String = "",
    userBio: String = "",
    posts: List<Post> = emptyList(),
    followersCount: Int = 0,
    followingCount: Int = 0,
    savedPosts: List<Post> = emptyList(),
    taggedBills: List<LinkedBillSummary> = emptyList(),
    isLoggingOut: Boolean = false,
    isPublic: Boolean = true,
    isSettingsDialogOpen: Boolean = false,
    onCloseSettings: () -> Unit = {},
    bottomPadding: Dp = 80.dp,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onPrivacyChange: (Boolean) -> Unit = {},
    onOpenSearch: () -> Unit,
    onLogout: () -> Unit,
    onOpenPostDetail: (String) -> Unit = {},
    onBillClick: (String, String) -> Unit = { _, _ -> },
    onNavigateToFollowList: (Int) -> Unit = {},
) {
    val resolvedHandle = userHandle.ifBlank { "@" }
    val resolvedBio = userBio.ifBlank { "Add a bio so friends know who they are splitting with." }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
 
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isLoggingOut) showLogoutConfirmation = false },
            title = { Text("Đăng xuất?") },
            text = { Text("Bạn sẽ quay lại màn hình đăng nhập.") },
            confirmButton = {
                TextButton(
                    enabled = !isLoggingOut,
                    onClick = {
                        showLogoutConfirmation = false
                        onLogout()
                    },
                ) {
                    Text("Đăng xuất")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLoggingOut,
                    onClick = { showLogoutConfirmation = false },
                ) {
                    Text("Hủy")
                }
            },
        )
    }
 
    if (isSettingsDialogOpen) {
        AlertDialog(
            onDismissRequest = onCloseSettings,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text("Cài đặt riêng tư")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                    Text(
                        text = "Thiết lập chế độ hiển thị tài khoản của bạn. Khi tài khoản là công khai, mọi người đều có thể xem bài viết của bạn. Khi tắt chế độ này, chỉ bạn bè (những người cùng theo dõi nhau) mới xem được.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tài khoản Công khai",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Switch(
                            checked = isPublic,
                            onCheckedChange = onPrivacyChange
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onCloseSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Đóng", color = Color.White)
                }
            }
        )
    }
 
    Scaffold { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            ProfileHeader(
                userName = resolvedHandle,
                displayName = userName,
                bio = resolvedBio,
                link = "",
                posts = posts.size.toString(),
                followers = followersCount.toString(),
                following = followingCount.toString(),
                avatarUrl = userAvatarUrl.orEmpty(),
                onEditProfile = onEditProfile,
                isLoggingOut = isLoggingOut,
                onLogout = { showLogoutConfirmation = true },
                onFollowingClick = { onNavigateToFollowList(0) },
                onFollowersClick = { onNavigateToFollowList(1) }
            )

            ProfileTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

            when (selectedTab) {
                0 -> {
                    val postsWithImages = posts.filter { it.imageUrls.isNotEmpty() }
                    if (postsWithImages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppDimens.space4Xl),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline.copy(0.6f),
                                    modifier = Modifier.size(AppDimens.space4Xl)
                                )
                                Text(
                                    text = "Chưa có bài đăng nào",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        PhotoGrid(posts = posts, onPostClick = onOpenPostDetail)
                    }
                }
                1 -> {
                    val savedWithImages = savedPosts.filter { it.imageUrls.isNotEmpty() }
                    if (savedWithImages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppDimens.space4Xl),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline.copy(0.6f),
                                    modifier = Modifier.size(AppDimens.space4Xl)
                                )
                                Text(
                                    text = "Chưa có bài viết đã lưu",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        PhotoGrid(posts = savedPosts, onPostClick = onOpenPostDetail)
                    }
                }
                2 -> {
                    TaggedBillsList(bills = taggedBills, onBillClick = onBillClick)
                }
            }

            Spacer(modifier = Modifier.height(bottomPadding + 40.dp))
        }
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
    displayName: String,
    bio: String,
    link: String,
    posts: String,
    followers: String,
    following: String,
    avatarUrl: String,
    onEditProfile: () -> Unit,
    isLoggingOut: Boolean,
    onLogout: () -> Unit,
    onFollowingClick: () -> Unit,
    onFollowersClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(AppDimens.spaceXl)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXl),
        ) {
            // Avatar with Gradient Ring
            Box(
                modifier =
                    Modifier
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
                    imageUrl = avatarUrl,
                    name = displayName,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .border(AppDimens.radiusSm, MaterialTheme.colorScheme.background, CircleShape),
                    size = 94.dp,
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)) {
                Text(userName, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))

                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                    PrimaryButton(
                        text = "Edit Profile",
                        onClick = onEditProfile,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceXl))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space2Xl),
        ) {
            StatItem(label = "Posts", value = posts)
            StatItem(
                label = "Followers",
                value = followers,
                modifier = Modifier.clickable { onFollowersClick() }
            )
            StatItem(
                label = "Following",
                value = following,
                modifier = Modifier.clickable { onFollowingClick() }
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceLg))

        // Info
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                link,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SecondaryButton(
            text = if (isLoggingOut) "Đang đăng xuất..." else "Đăng xuất",
            onClick = onLogout,
            enabled = !isLoggingOut,
            modifier = Modifier.fillMaxWidth(),
            icon = {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun StatItem(
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
private fun ProfileOverview() {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
    ) {
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                Text("Profile activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Posts, saved meals, and tagged splits will appear here as real activity is added.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProfileTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val tabs =
        listOf(
            TabInfo("Grid", Icons.Default.GridView),
            TabInfo("Saved", Icons.Default.BookmarkBorder),
            TabInfo("Tagged", Icons.Default.AccountBox),
        )
    val primaryColor = MaterialTheme.colorScheme.primary

    Column {
        Row(modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight)) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabSelected(index) }
                            .drawBehind {
                                if (isSelected) {
                                    drawLine(
                                        color = primaryColor,
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                        strokeWidth = 2.dp.toPx(),
                                    )
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(AppDimens.spaceXl),
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

@Composable
private fun PhotoGrid(
    posts: List<Post>,
    onPostClick: (String) -> Unit,
) {
    val postsWithImages = posts.filter { it.imageUrls.isNotEmpty() }
    Column(modifier = Modifier.padding(horizontal = AppDimens.spaceXs)) {
        val rows = postsWithImages.chunked(3)
        rows.forEach { rowPosts ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.spaceXs / 2), horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
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
                // Fill empty slots if last row has less than 3 photos
                repeat(3 - rowPosts.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class TabInfo(val label: String, val icon: ImageVector)

@Composable
private fun TaggedBillsList(
    bills: List<LinkedBillSummary>,
    onBillClick: (String, String) -> Unit
) {
    if (bills.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.space4Xl),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(0.6f),
                    modifier = Modifier.size(AppDimens.space4Xl)
                )
                Text(
                    text = "Chưa có hoạt động chia tiền nào",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            bills.forEach { summary ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = AppShapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBillClick(summary.groupId, summary.billId) }
                ) {
                    Column(
                        modifier = Modifier.padding(AppDimens.spaceLg),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                      ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
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
                                    modifier = Modifier.padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceXs / 2),
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
                                    text = formatMoney(summary.totalAmount),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (summary.isIPayer) "Bạn đã trả trước" else "Phần của bạn",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatMoney(summary.myShare),
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

private fun formatMoney(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return formatter.format(amount)
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        ProfileScreen(
            userAvatarUrl = null,
            userName = "Linh Trần",
            isLoggingOut = false,
            onEditProfile = {},
            onOpenSettings = {},
            onOpenSearch = {},
            onLogout = {},
        )
    }
}
