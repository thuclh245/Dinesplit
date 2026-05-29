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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.HomeTopBar
import com.example.dinesplit.ui.theme.DineSplitTheme

@Composable
fun ProfileScreen(
    userAvatarUrl: String?,
    userName: String,
    userHandle: String = "",
    userBio: String = "",
    isLoggingOut: Boolean = false,
    isSeeding: Boolean = false,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onSeedDemoData: () -> Unit = {},
    onOpenSearch: () -> Unit,
    onLogout: () -> Unit
) {
    val resolvedHandle = userHandle.ifBlank { "@" }
    val resolvedBio = userBio.ifBlank { "Add a bio so friends know who they are splitting with." }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var wasSeeding by remember { mutableStateOf(false) }

    LaunchedEffect(isSeeding) {
        if (isSeeding) {
            wasSeeding = true
        } else if (wasSeeding) {
            showSettingsDialog = false
            wasSeeding = false
        }
    }

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
                    }
                ) {
                    Text("Đăng xuất")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLoggingOut,
                    onClick = { showLogoutConfirmation = false }
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSeeding) showSettingsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Cài đặt Nhà phát triển")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Gieo dữ liệu mẫu để trải nghiệm đầy đủ các tính năng của ứng dụng (bao gồm giao dịch cá nhân, thông báo và các bài đăng trên Feed).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isSeeding) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Đang gieo dữ liệu mẫu vào Firestore...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onSeedDemoData,
                    enabled = !isSeeding,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Gieo dữ liệu", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSettingsDialog = false },
                    enabled = !isSeeding
                ) {
                    Text("Đóng")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                userAvatarUrl = userAvatarUrl,
                title = userName,
                onOpenSearch = onOpenSearch
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHeader(
                userName = resolvedHandle,
                displayName = userName,
                bio = resolvedBio,
                link = "",
                posts = "4",
                followers = "142",
                following = "89",
                avatarUrl = userAvatarUrl.orEmpty(),
                onEditProfile = onEditProfile,
                onOpenSettings = { showSettingsDialog = true },
                isLoggingOut = isLoggingOut,
                onLogout = { showLogoutConfirmation = true }
            )

            ProfileTabs()

            ProfileOverview()
            
            Spacer(modifier = Modifier.height(120.dp))
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
    onOpenSettings: () -> Unit,
    isLoggingOut: Boolean,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Avatar with Gradient Ring
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)), CircleShape)
                    .padding(3.dp)
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().border(4.dp, MaterialTheme.colorScheme.background, CircleShape).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(userName, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onEditProfile,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Edit Profile", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    Surface(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            StatItem(label = "Posts", value = posts)
            StatItem(label = "Followers", value = followers)
            StatItem(label = "Following", value = following)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(displayName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Text(bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
            Text(link, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onLogout,
            enabled = !isLoggingOut,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoggingOut) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isLoggingOut) "Đang đăng xuất..." else "Đăng xuất")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ProfileOverview() {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
    ) {
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                Text("Profile activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Posts, saved meals, and tagged splits will appear here as real activity is added.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileTabs() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        TabInfo("Grid", Icons.Default.GridView),
        TabInfo("Saved", Icons.Default.BookmarkBorder),
        TabInfo("Tagged", Icons.Default.AccountBox)
    )

    Column {
        Row(modifier = Modifier.fillMaxWidth().height(56.dp)) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedTab = index }
                        .drawBehind {
                            if (isSelected) {
                                drawLine(
                                    color = Color(0xFFAB2D00),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

@Composable
private fun PhotoGrid(photos: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        val rows = photos.chunked(3)
        rows.forEach { rowPhotos ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowPhotos.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                // Fill empty slots if last row has less than 3 photos
                repeat(3 - rowPhotos.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class TabInfo(val label: String, val icon: ImageVector)
private val samplePhotos = listOf(
    "https://lh3.googleusercontent.com/aida-public/AB6AXuDPo68lKRJUgLSRbIMyP8cKangAwx0j7hXe8PGwHVAeN_TDWusH8I8piDCWfw0boUfkdYsVUlUlUV1YD0w-MrFAyUSQZOSjFcFJ3NbqdUSOKrAk0ubOLr-Rb7iQTBtNH2gNKYkNca8ETj4zX1WjUd40GlfZOPbOWsxFuGpuAdi-99aZ5hRzAZisrBwQcfUUsqOZP-3qNcUYWf_t1vgCRaRczPZhkyd8-snCNxnXflJ5wz15MmXNLuTvCeyYY2MBT3AHVoVEv-t-s9s",
    "https://lh3.googleusercontent.com/aida-public/AB6AXuAFcJh_WZqVCMMZv3NBDAbKn8TO2X9FTtTgdKQrSY01kiKSI0JS0mmvY-Fe4EL6Ku_O4yvkEXB2mXSAM7wcQbmJAcUYvX1382IgqqR9Sq9f-51QR9wlqbC-YeyL69KaKzKccU1OaWX881J_GofRqtcPQME54kWADXnILsPIK75U4kP07KJs8nLTmrED_azhBJdiCUAeamAiG3NyDKOIhuxuQ57cI9zzYj2xFFX2nG8A0bsHw61VTKaQLdAz_lkHX5HqB5piLy7y02U",
    "https://lh3.googleusercontent.com/aida-public/AB6AXuBfGVqZEjq3TzaADMZ2buEd9tcHuiLkAoFK4FbRhL6Y9VkMGlV6y1zIRrYCn-olI46CLic4shUSoA5v49slK5WqMT6yMbuI-0_BmNRtEq3rJNb27iTHhck0QjS9faAp-Y14z_oJX4xAIAZ5NYCaYr8fO0gOgD3R0OXTMTEGuGB92iTGChFBAaMkmTpEsg_xoiZ0DRo-XPvIcOrSndlelqCjlo3RxgBFaT-BjHSpRZgHWrZA3pWRbPUd5qnxazk9I3bzJX4jBfI83JU",
    "https://lh3.googleusercontent.com/aida-public/AB6AXuC32dimkr7Rmci-xva5bmegZsbng43iQe9X5By2wvKya393xmrteEaed26ylxNdfRDepdEL34vta7dPuvfZTCMFQF_kpvrRFV8ZowU6rmReLh3whaosUh0wGou5w0XIsDyiNATIMCHUMr5-icroJ9GN-ebRm083ZBZLIzxm8nIjgVenrYOO05IhtnzH9CG27mL127oQAbOKOb6vzde0yXhO4ZE4bSL-qsdbeLdmme2kRc1a7v8-lLD_L7ME3DqCIwtz4BaA3WjVFHs",
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCz15-drOE04YNhXjKak_xexU7l7z5yKLDOq2-rnnNIo-GY-Jn--zxEi9VmocpjT4cbiJ2W4ONCPbrnGQ15A7yhQkPpUiCKjHbDiEe-DJoHxAelIkVsh078_KRSnkSJKNdw5vDuKOtri72kPncg8pYkGE74MRIzjyHxbI255p7Y9oP6ogKpoJHhcwd_ZZAoc9SgtfaDvtY2e6-hTKygHmaToOL8LVHHLuzQyMMFqju90iv0rD9-926Dq-5DMAAzHncrfLvhnxKxpxs",
    "https://lh3.googleusercontent.com/aida-public/AB6AXuDqhcDBDtkEkv9k095DhXUETbPEu2BmjWo0sOxInwgyuJ39Ctwm_V5bOa4Mc3_h0VxBNQYiFWMOOxMkJpGs2fiMOBjatAjj6NwFVyKXT2XM_WJikSgcUQLuShbh8jUgobtbfNkwVMUTRtPc1IxHED07NKI1wvZatqbjoBgMPYzHbRDff8KBFqKW4AQsdCpv5mmdCmMtV6C6-DZR2tIrs6eKMHv7sWF1JO-G8IQ0Eu68Hnc2tjAIuDd8Mq02nZPSHsIpd1dt6qW-r1k"
)

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
            onLogout = {}
        )
    }
}
