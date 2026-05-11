package com.example.dinesplit.presentation.split

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dinesplit.ui.theme.DineSplitTheme
import com.example.dinesplit.core.ui.HomeTopBar

@Composable
fun SplitScreen(
    userAvatarUrl: String?,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onNewGroup: () -> Unit,
    onNewExpense: () -> Unit
) {
    Scaffold(
        topBar = {
            HomeTopBar(
                userAvatarUrl = userAvatarUrl,
                title = "Split Bill",
                onOpenNotifications = onOpenNotifications,
                onOpenSearch = onOpenSearch
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .size(64.dp)
                    .shadow(24.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Balance Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), 
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BalanceCard(
                        title = "BẠN ĐANG NỢ",
                        amount = "450.000",
                        buttonText = "Settle Up",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                    BalanceCard(
                        title = "BẠN ĐƯỢC TRẢ",
                        amount = "1.280.000",
                        buttonText = "Remind",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        showLeftBorder = true
                    )
                }
            }

            // Groups Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "Nhóm của bạn",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            "Xem tất cả",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            GroupCard(
                                name = "Biệt đội lẩu nướng",
                                membersCount = 5,
                                status = "8 hóa đơn mới",
                                avatars = listOf(
                                    "https://lh3.googleusercontent.com/aida-public/AB6AXuDY_IMS_eMe1WwjdfdlS1m9N0Vy7mmSj6IievjyBueIY4HCA6WMaT__vvYkQ3ICvjABqTPwQh9iuqvmyl2vf9v8TQ8wyYxkV4BwU_uWvJo79ZpRUhSIHX0B2FPxtKrxE-vAB-K7jr63Di6wV7OrsykGZasoAxJTwltKvEeNifFBvAyxS5sJhwzncrv42nDw2BtqgR7F2lO0BXlYNHF4HGhdyrnvZPAXrqPI3H6BHndN5CSvMKRuvMrSPSrncucKvO0CcxXB38aLWco",
                                    "https://lh3.googleusercontent.com/aida-public/AB6AXuAfYcO9jIaHFAy40rvG3ZeumiPaCSHqM8JklUt4-f3AFz1YuReCTFOxdRhc7tl18jk-P44QArUL3DsJ7EFTb0HJxVz2R3HQsHBZsk8-7BJLmaG8IClGKpMvMXT1Ul2gsWcPSP0zt5tLR7PofI8l0LRt20-UYnJ0SQg2y__LUNgCzG49LyqddGaTl_s3TYn-tew8fOGe6Nbhbg6QHOLpA9us25y_3J1HJ46YPlqDSsqjXq6maFBn6W67EhvDKbSrvDkHeGQhFlGDnF0"
                                ),
                                extraMembers = 3,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        }
                        item {
                            GroupCard(
                                name = "Gia đình vui vẻ",
                                membersCount = 2,
                                status = "Đã tất toán",
                                isSettled = true,
                                avatars = listOf(
                                    "https://lh3.googleusercontent.com/aida-public/AB6AXuA7gL19DfBLY4ZF-lgXkphKLVIYRaxB7Lz0QS4bc3VO47YvnBKTjpY--NH2LyOm-Jx6fJRyyQpIwSxHYKNFEhSlm0Zg07v7RXXB8rnYpeNeaSoQmI8MQHE3XaTbbkNfPDJwUWAQBGU7pjM9invkWYs9RiYG47luy0M5hvggOXwIJm-46KGeUUG7gZBk5EIWzM49lSD3rY9lnGz2iVw2__XEVHRniOcfB1Ch_x7B1xfv71Ytwjz9P25RN16WDti0-KOINCvtltNeh-c",
                                    "https://lh3.googleusercontent.com/aida-public/AB6AXuDn6mQNJRxPZ_JecfKdipvnL6MZMH2Eo4oNZnVD8QI5QcvFGv7MdFH1DpAnTSABYh1bA6Di5-qsPK4SRvonyYUGHYz0YIf7jZE2k2LHQm4i97ffUv4NMGlGoSZQ-BjgmvEYg43CV68P9-IQrsggCqcmahCqpt80GS8VegSmkgqVwWAhGUPZyMsBEoG6-_o1vrq5LQMOabSV6OBwfetSC2oRew69dYSP7cidixg7E2nH2QHJMVE1n-9wUFL5zotw2sXPY3FsJf7vzHQ"
                                ),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .width(240.dp)
                                    .height(192.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onNewGroup() },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                                    Text("Tạo nhóm mới", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Bills
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Hóa đơn gần đây",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BillItem(
                            title = "Haidilao Vincom",
                            subtitle = "Hôm qua • Biệt đội lẩu nướng",
                            amount = "Bạn nợ 250k",
                            status = "OPEN",
                            icon = Icons.Default.Restaurant,
                            iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            iconColor = MaterialTheme.colorScheme.primary,
                            isNegative = true
                        )
                        BillItem(
                            title = "Starbucks Reserve",
                            subtitle = "15 thg 10 • Cá nhân",
                            amount = "Đã trả 120k",
                            status = "SETTLED",
                            icon = Icons.Default.LocalCafe,
                            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                            iconColor = MaterialTheme.colorScheme.tertiary
                        )
                        BillItem(
                            title = "Đi chợ cuối tuần",
                            subtitle = "12 thg 10 • Gia đình vui vẻ",
                            amount = "Nhận 540k",
                            status = "SETTLED",
                            icon = Icons.Default.Payments,
                            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            iconColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(
    title: String,
    amount: String,
    buttonText: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    showLeftBorder: Boolean = false
) {
    Card(
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showLeftBorder) {
                Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(contentColor).align(Alignment.CenterStart))
            }
            Column(
                modifier = Modifier.padding(20.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.outlineVariant)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(amount, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = contentColor)
                        Text("đ", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = contentColor, modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                    }
                }
                Button(
                    onClick = { },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showLeftBorder) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        contentColor = if (showLeftBorder) MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                    ),
                    modifier = Modifier.then(
                        if (!showLeftBorder) Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)), CircleShape)
                        else Modifier
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(buttonText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    name: String,
    membersCount: Int,
    status: String,
    avatars: List<String>,
    extraMembers: Int = 0,
    isSettled: Boolean = false,
    containerColor: Color
) {
    Card(
        modifier = Modifier.width(240.dp).height(192.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        avatars.forEach { url ->
                            AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(32.dp).border(2.dp, Color.White, CircleShape).clip(CircleShape), contentScale = ContentScale.Crop)
                        }
                        if (extraMembers > 0) {
                            Box(modifier = Modifier.size(32.dp).border(2.dp, Color.White, CircleShape).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                                Text("+$extraMembers", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$membersCount thành viên", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isSettled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = if (isSettled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = if (isSettled) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSettled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BillItem(
    title: String,
    subtitle: String,
    amount: String,
    status: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconColor: Color,
    isNegative: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).background(iconContainerColor, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                Surface(
                    color = if (isNegative) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                        color = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplitScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        SplitScreen(
            userAvatarUrl = null,
            onOpenNotifications = {},
            onOpenSearch = {},
            onNewGroup = {},
            onNewExpense = {}
        )
    }
}
