package com.example.dinesplit.presentation.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.DineGridImage
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton
import com.example.dinesplit.core.ui.SmallButton
import com.example.dinesplit.core.ui.TertiaryButton
import com.example.dinesplit.domain.model.LinkedBillSummary
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.ui.theme.DineSplitTheme
import java.text.NumberFormat
import java.util.Locale


/**
 * Màn hình Hồ sơ cá nhân (Profile Screen)
 * Hiển thị thông tin cá nhân của người dùng, thống kê, bài viết đã đăng, bài viết đã lưu,
 * và các hóa đơn chia tiền được gắn thẻ. Đồng thời hỗ trợ thiết lập quyền riêng tư và đăng xuất.
 */
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
    isNotificationsEnabled: Boolean = true,
    isNotificationsMutedPermanently: Boolean = false,
    muteUntilTimestamp: Long = 0L,
    onNotificationsEnabledChange: (Boolean) -> Unit = {},
    onMutePermanentlyChange: (Boolean) -> Unit = {},
    onMuteUntilChange: (Int) -> Unit = {},
) {
    // Xử lý các giá trị mặc định cho Handle và Bio nếu bị trống
    val resolvedHandle = userHandle.ifBlank { "@" }
    val resolvedBio = userBio.ifBlank { "Add a bio so friends know who they are splitting with." }
    
    // Trạng thái hiển thị Dialog xác nhận đăng xuất
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    // Tab đang được chọn: 0 - Bài đăng, 1 - Đã lưu, 2 - Hóa đơn được gắn thẻ
    var selectedTab by remember { mutableStateOf(0) }
 
    // Dialog xác nhận Đăng xuất
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isLoggingOut) showLogoutConfirmation = false },
            title = { Text("Đăng xuất?") },
            text = { Text("Bạn sẽ quay lại màn hình đăng nhập.") },
            confirmButton = {
                TertiaryButton(
                    text = "Đăng xuất",
                    enabled = !isLoggingOut,
                    onClick = {
                        showLogoutConfirmation = false
                        onLogout()
                    },
                )
            },
            dismissButton = {
                TertiaryButton(
                    text = "Hủy",
                    enabled = !isLoggingOut,
                    onClick = { showLogoutConfirmation = false },
                )
            },
        )
    }
 
    // Dialog Cài đặt & Riêng tư
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
                    Text("Cài đặt & riêng tư")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                    Text(
                        text = "Thiết lập chế độ hiển thị tài khoản của bạn. Khi tài khoản là công khai, mọi người đều có thể xem bài viết của bạn. Khi tắt chế độ này, chỉ bạn bè (những người cùng theo dõi nhau) mới xem được.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                    
                    // Nút chuyển trạng thái tài khoản Công khai / Riêng tư
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = isPublic,
                                role = Role.Switch,
                                onValueChange = onPrivacyChange
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tài khoản Công khai",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Switch(
                            checked = isPublic,
                            onCheckedChange = null
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = AppDimens.spaceSm))

                    Text(
                        text = "Cài đặt thông báo",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Nút chuyển trạng thái Nhận thông báo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = isNotificationsEnabled,
                                role = Role.Switch,
                                onValueChange = onNotificationsEnabledChange
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Nhận thông báo",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Switch(
                            checked = isNotificationsEnabled,
                            onCheckedChange = null
                        )
                    }
                }
            },
            confirmButton = {
                SmallButton(
                    text = "Đóng",
                    onClick = onCloseSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                )
            }
        )
    }
 
    // Giao diện chính của tab Hồ sơ
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
            
            // Header hồ sơ chứa Avatar, Tên, Bio, và Thống kê nhanh
            ProfileHeader(
                userName = resolvedHandle,
                displayName = userName,
                bio = resolvedBio,
                link = "",
                posts = posts.size.toString(),
                followers = followersCount.toString(),
                following = followingCount.toString(),
                taggedBillsCount = taggedBills.size,
                avatarUrl = userAvatarUrl.orEmpty(),
                onEditProfile = onEditProfile,
                isLoggingOut = isLoggingOut,
                onLogout = { showLogoutConfirmation = true },
                onFollowingClick = { onNavigateToFollowList(0) },
                onFollowersClick = { onNavigateToFollowList(1) }
            )

            // Thanh điều hướng Tab bên dưới Header
            ProfileTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

            // Nội dung thay đổi tương ứng theo Tab đang chọn
            when (selectedTab) {
                0 -> { // Tab 0: Danh sách bài viết của chính người dùng
                    val postsWithImages = posts.filter { it.imageUrls.isNotEmpty() }
                    if (postsWithImages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp, bottom = 48.dp),
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
                1 -> { // Tab 1: Danh sách bài viết người dùng đã lưu lại
                    val savedWithImages = savedPosts.filter { it.imageUrls.isNotEmpty() }
                    if (savedWithImages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp, bottom = 48.dp),
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
                2 -> { // Tab 2: Danh sách các hoá đơn chia tiền được tag tên vào
                    TaggedBillsList(bills = taggedBills, onBillClick = onBillClick)
                }
            }

            Spacer(modifier = Modifier.height(bottomPadding + 40.dp))
        }
    }
}

/**
 * Component hiển thị thông tin phần đầu trang cá nhân (Profile Header)
 * Bao gồm Avatar (với viền gradient màu thương hiệu), Tên hiển thị, Username, Bio,
 * Nút "Chỉnh sửa hồ sơ", Thống kê người theo dõi/bài viết, Card Thống kê DineSplit và nút Đăng xuất.
 */
@Composable
private fun ProfileHeader(
    userName: String,
    displayName: String,
    bio: String,
    link: String,
    posts: String,
    followers: String,
    following: String,
    taggedBillsCount: Int,
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
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            // Ảnh đại diện (Avatar) với viền tròn màu Gradient
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

            // Username và nút Chỉnh sửa hồ sơ
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                Text(userName, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))

                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                    OutlinedButton(
                        onClick = onEditProfile,
                        modifier = Modifier.height(32.dp),
                        shape = AppShapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Chỉnh sửa hồ sơ",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceXl))

        // Chỉ số Thống kê cơ bản: Bài viết, Người theo dõi, Đang theo dõi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space2Xl),
        ) {
            StatItem(label = "Bài viết", value = posts)
            StatItem(
                label = "Người theo dõi",
                value = followers,
                modifier = Modifier.clickable { onFollowersClick() }
            )
            StatItem(
                label = "Đang theo dõi",
                value = following,
                modifier = Modifier.clickable { onFollowingClick() }
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceLg))

        // Thông tin Bio cá nhân
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

        Spacer(modifier = Modifier.height(AppDimens.spaceLg))

        // Thẻ Thống kê chi tiêu & kết nối (DineSplit Stats Card)
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(AppDimens.spaceMd),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                Text(
                    text = "THỐNG KÊ DINESPLIT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Số lượng bài đăng về món ăn
                    DineSplitStatColumn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Restaurant,
                        value = posts,
                        label = "Bài đăng",
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                    
                    // Số lượng hóa đơn chia tiền người dùng tham gia
                    DineSplitStatColumn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        value = taggedBillsCount.toString(),
                        label = "Hóa đơn chia",
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                    
                    // Tổng số kết nối (Người theo dõi + Đang theo dõi)
                    DineSplitStatColumn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.People,
                        value = ((followers.toIntOrNull() ?: 0) + (following.toIntOrNull() ?: 0)).toString(),
                        label = "Kết nối",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Nút Đăng xuất
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
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
    }
}

/**
 * Cột hiển thị một chỉ số thống kê đặc thù của DineSplit (Bài đăng, Hóa đơn chia, Kết nối)
 */
@Composable
private fun DineSplitStatColumn(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Một mục hiển thị số lượng thống kê cơ bản ở đầu trang (bài viết, người theo dõi...)
 */
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

/**
 * Thanh chuyển đổi Tab trong trang cá nhân: Bài đăng, Đã lưu, Được gắn thẻ.
 */
@Composable
private fun ProfileTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val tabs =
        listOf(
            TabInfo("Bài đăng", Icons.Default.GridView),
            TabInfo("Đã lưu", Icons.Default.BookmarkBorder),
            TabInfo("Được gắn thẻ", Icons.Default.AccountBox),
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
                                // Vẽ đường line màu thương hiệu ở dưới tab được chọn
                                if (isSelected) {
                                    drawLine(
                                        color = primaryColor,
                                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                                        strokeWidth = 3.dp.toPx(),
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

/**
 * Grid hiển thị ảnh các bài viết (chia làm 3 cột trên một hàng).
 */
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
                // Thêm khoảng trống nếu hàng cuối cùng có ít hơn 3 ảnh
                repeat(3 - rowPosts.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class TabInfo(val label: String, val icon: ImageVector)

/**
 * Danh sách các hóa đơn chia tiền được gắn thẻ (Tagged Bills List).
 * Hiển thị thông tin tổng tiền, phần tiền của người dùng và trạng thái thanh toán.
 */
@Composable
private fun TaggedBillsList(
    bills: List<LinkedBillSummary>,
    onBillClick: (String, String) -> Unit
) {
    if (bills.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
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
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
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
                            
                            // Nhãn trạng thái: Đã thanh toán (Màu Secondary) / Chưa thanh toán (Màu Error)
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
                            // Tổng số tiền trên hóa đơn
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
                            
                            // Số tiền người dùng này cần trả (hoặc đã ứng trước)
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

/**
 * Hàm hỗ trợ format tiền tệ theo VND (vi-VN)
 */
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
