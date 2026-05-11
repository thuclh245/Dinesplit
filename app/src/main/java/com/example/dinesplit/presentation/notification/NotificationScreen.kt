package com.example.dinesplit.presentation.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dinesplit.ui.theme.DineSplitTheme
import androidx.compose.foundation.border

@Composable
fun NotificationScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            NotificationTopBar(onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SectionHeader("New")
            }
            
            item {
                NotificationItem(
                    title = "New Bill from Alex",
                    description = "\"Dinner at The Rusty Spoon\" - You owe $42.50",
                    time = "2m ago",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    isUnread = true,
                    actionText = "Pay",
                    onAction = { }
                )
            }

            item {
                NotificationItem(
                    userName = "Sarah",
                    userAvatar = "https://lh3.googleusercontent.com/aida-public/AB6AXuBpK7BPXAuysJQL2nfbAJQ1GKz-diPYf5lAb53xUH-rK2dH2n4XxbgAVc-ZvlmlPX39x8AmklP45XTjVRjBfh3Tu-bKPzpkGAJOGo2IRzjy6QzNwEE5rJN2df32yNAjMuUPDFnC61sCmWNjzPJXVvysF6v69LVm4RkA8sDfl0ri015K5X1MdQeUWr2buZ4OaQninIdNDeHB6aX95rLJCsD9dQ_do0nO4vkIRPI7z1hfwaxfP93OlYwYTuvhXuNO7t9apSSoriROTg",
                    socialAction = "liked your group dinner",
                    description = "\"Sushi Night with the team\"",
                    time = "15m ago",
                    isUnread = true
                )
            }

            item {
                SectionHeader("Earlier")
            }

            item {
                NotificationItem(
                    title = "Payment Received",
                    description = "Mike sent you $25.00 for \"Coffee Run\"",
                    time = "2h ago",
                    icon = Icons.Default.CheckCircle,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    isUnread = false
                )
            }

            item {
                NotificationItem(
                    userName = "David",
                    userAvatar = "https://lh3.googleusercontent.com/aida-public/AB6AXuC9ppqQ48FQPH5UT64mPRgJ_djb4ZIfvWdCwD4mahtuxqBLSwFu4USsB7_W1t6zFIpReRZCb0syvQbT0FZwv2QhWzNdlUiFrxzyw8fLVGkU5i-fJBfhkk64RqlHQng6ifGC8TZsV-PUcjYHQ-_HDA1QtZhIlgHCsZ0SFvQZuzI1LbJNQkxt1pjvUNV4SJp-1KTqpTjBp_lHJp-bYjy8BAQpC2T66QQvIXc0ZUf485vmlB6FNpImfu_LOm4UbumgG10ZEeDaAK3o5Q",
                    socialAction = "commented on your split",
                    description = "\"Thanks for covering tip!\"",
                    isItalicDescription = true,
                    time = "Yesterday",
                    isUnread = false
                )
            }

            item {
                NotificationItem(
                    title = "Monthly Summary Ready",
                    description = "View your spending for October.",
                    time = "Oct 31",
                    icon = Icons.Default.AccountBalance,
                    isUnread = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "Notifications",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
        ),
        modifier = Modifier.shadow(1.dp)
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun NotificationItem(
    title: String? = null,
    userName: String? = null,
    userAvatar: String? = null,
    socialAction: String? = null,
    description: String,
    isItalicDescription: Boolean = false,
    time: String,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    isUnread: Boolean = false,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth().alpha(if (isUnread) 1.0f else 0.75f),
        shape = RoundedCornerShape(12.dp),
        color = if (isUnread) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box {
            if (isUnread) {
                Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(4.dp).background(MaterialTheme.colorScheme.primary))
            }
            
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon or Avatar
                if (userAvatar != null) {
                    AsyncImage(
                        model = userAvatar,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else if (icon != null) {
                    Box(
                        modifier = Modifier.size(48.dp).background(iconTint.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                    }
                }

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    if (userName != null) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(userName) }
                                append(" ")
                                append(socialAction ?: "")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = if (isItalicDescription) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Action
                if (actionText != null && onAction != null) {
                    Button(
                        onClick = onAction,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(actionText, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        NotificationScreen(onBack = {})
    }
}
