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
import com.example.dinesplit.ui.theme.DineSplitTheme

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit
) {
    Scaffold(
        topBar = {
            ProfileTopBar(userName = "Linh Trần", onOpenSearch = onOpenSearch)
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
                userName = "linh_eats_saigon",
                displayName = "Linh Trần",
                bio = "Chasing flavors across the city 🍜\nSaigon | Food Explorer | Split Enthusiast",
                link = "linktr.ee/linh_eats",
                posts = "128",
                followers = "4.2k",
                following = "842",
                avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuA-suY-k2Nl-qCL-4wnA1hg-m6zuH1YcCMkIHMSe5mSzui6H4g6m2PhsF-EquINcQ40evSvEhwOFKKVD-f-AvP6yrvuLjY9ZOh2-13J-qRHgqN9S3NzBeLgm2Podb878O6hsHapq8VpaIvfgYsZ9cSNhk2GBQYJbLCYlpijNeSOu8fjq_ayNvVXxeTRJWQiHqVXcSClUiWQN24CcQZ6mEsc-3RuatfZuA8RIi3yVazdggBJbeS8GfpL9f0ai0sgfe-XYv59XOoYBzQ",
                onEditProfile = onEditProfile,
                onOpenSettings = onOpenSettings
            )

            ProfileTabs()

            // Photo Grid (Simplified for scrollable column)
            PhotoGrid(photos = samplePhotos)
            
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun ProfileTopBar(userName: String, onOpenSearch: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuAl4p5h_h5ljcVfoW054Dwb67nrtGLfzS4MG6T7JiC6tThrZxmPYLdmkTS0bsLy10DR7XhIfTlSJxfJmhIkLO5NWBcOH3MgY1PLdiLvDZwHTRp5srVNy3dUcjkmWFw72qDzxmA8SZsbg7FjxoR8fia13mJ0Aghmzhjz5-QtxJX0EZbBSoyxcGmjTKLGklYb0GA7jQRQasevQHL84z6OxsdftShugm9OXp_E_m4kgPxgbnyy33rafAQBooVBJZ-HUGYJ8XeXFAvkvNo",
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceContainerLow))
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
    onOpenSettings: () -> Unit
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
            onEditProfile = {},
            onOpenSettings = {},
            onOpenSearch = {}
        )
    }
}
