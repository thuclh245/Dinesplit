package com.example.dinesplit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        modifier = Modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        navigationIcon = {
            navigationIcon?.invoke()
        },
        actions = actions,
        colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

@Composable
fun BackNavigationButton(
    onClick: () -> Unit,
    contentDescription: String = "Quay lại",
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
        )
    }
}

/**
 * Unified Home Top Bar for major sections (Feed, Split, Personal, Profile).
 * Standardizes the "H1" header with profile avatar and consistent naming.
 */
@Composable
fun HomeTopBar(
    userAvatarUrl: String?,
    title: String = "DineSplit",
    onAvatarClick: () -> Unit = {},
    onOpenAssistant: (() -> Unit)? = null,
    onOpenChats: (() -> Unit)? = null,
    onOpenSearch: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    notificationUnreadCount: Int = 0,
    onOpenSettings: (() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(start = 24.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (title == "DineSplit") {
                // Feed Screen - Interactive Social Creator hub
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAvatarClick() }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clip(CircleShape),
                        ) {
                            AsyncImage(
                                model =
                                    userAvatarUrl.takeIf {
                                        !it.isNullOrBlank()
                                    } ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuCpnvw7NZghOMJRnH_WaC3eHutJm9XoZmybu_TS_uk9WGcWsJ_ROjtI90_bvBZh8RdgNB0TqYRJz9rZwQs8ccGh0XZdffsyr3NPpk2NVubfaS48U6sqwA-G3_MDzJUaOs2ZwR38m4yLqhn5qc9roHjyOG9DRe0snpGmqEaalIMhGPfnnWqyYIKfjwhLix41mqjPZc3XGCjrn-j-XR7ybsOoJqCMtxUDerTeRQZEdpJI07YwJUZ1l4qlO-YwxfFb6oJMhnd9dTple0c",
                                contentDescription = "Hồ sơ của tôi",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        // Beautiful small plus badge overlay
                        Box(
                            modifier =
                                Modifier
                                    .size(14.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(8.dp),
                            )
                        }
                    }
                    Text(
                        text = "DineSplit",
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                // Other Sections - Clean H1 standard layout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clip(CircleShape)
                                .clickable { onAvatarClick() },
                    ) {
                        AsyncImage(
                            model =
                                userAvatarUrl.takeIf {
                                    !it.isNullOrBlank()
                                } ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuCpnvw7NZghOMJRnH_WaC3eHutJm9XoZmybu_TS_uk9WGcWsJ_ROjtI90_bvBZh8RdgNB0TqYRJz9rZwQs8ccGh0XZdffsyr3NPpk2NVubfaS48U6sqwA-G3_MDzJUaOs2ZwR38m4yLqhn5qc9roHjyOG9DRe0snpGmqEaalIMhGPfnnWqyYIKfjwhLix41mqjPZc3XGCjrn-j-XR7ybsOoJqCMtxUDerTeRQZEdpJI07YwJUZ1l4qlO-YwxfFb6oJMhnd9dTple0c",
                            contentDescription = "Hồ sơ của tôi",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Text(
                        text = title,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onOpenAssistant != null) {
                    IconButton(onClick = onOpenAssistant) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Trợ lý DineSplit",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (onOpenChats != null) {
                    IconButton(onClick = onOpenChats) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Tin nhắn",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (onOpenSearch != null) {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (onOpenNotifications != null) {
                    IconButton(onClick = onOpenNotifications) {
                        BadgedBox(
                            badge = {
                                if (notificationUnreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    ) {
                                        Text(
                                            text = if (notificationUnreadCount > 99) "99+" else notificationUnreadCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription =
                                    if (notificationUnreadCount > 0) {
                                        "Thông báo, $notificationUnreadCount chưa đọc"
                                    } else {
                                        "Thông báo"
                                    },
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (onOpenSettings != null) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cài đặt",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
