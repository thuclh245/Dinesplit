package com.example.dinesplit.presentation.feed

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dinesplit.ui.theme.DineSplitTheme

@Composable
fun FeedScreen(
    userAvatarUrl: String?,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onSettleUp: (String) -> Unit
) {
    Scaffold(
        topBar = {
            FeedTopBar(userAvatarUrl, onOpenNotifications, onOpenSearch)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* New Bill */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .size(56.dp)
                    .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = "New Bill", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                RecentGroupVibes()
            }
            
            item {
                // Feed Card 1: Social Split
                SocialSplitCard(
                    userAvatar = "https://lh3.googleusercontent.com/aida-public/AB6AXuBtQH5RavTPzxY97sjOC1_C-5WWFTL32DxKbz_Z-tv9KUx1yxfISftiAJMjQ8QrgswscZTP1C0VYI1aNuaN0P-Rz1hQL6RWl88SNERhUBockH6Dkv1TaAvgSoGtZ4SITU_hD9RZboENDjETcmI8SkeF6dLxP9Q_GtBCWyAiVOFvflYFAM_IfEUl0es2g-2GCb-4s6GTwPyJ0HkmfqTsKmibwmCfdA1oOmrsmJQqAxGnkob4-ADWYTgEN_2y1BPjCqTiiCWEX6H2GWo",
                    userName = "Thanh Hằng",
                    location = "Bún Chả Hương Liên, Hà Nội",
                    mainImage = "https://lh3.googleusercontent.com/aida-public/AB6AXuDHwYXQfykJqzR1lK0J5v5yyPuixb1f3F8YgsbEvojUnJWloP0p-C53mROG_VFOdHsMReMytjIl6hgaGyMXNBBrL_W0NY__48UgZ_3UZ7nYLu4YJCgfmlo9MBaXx-JKmkGxkLnT0tfV1WCqC_7gqevnYNaHuQIaP4-zyR39-QKrMPu_WkuvjMZYEIUB9-Q-gahPawmuJ4zh_gXlUVGc3366-tdv9lvlhYtrCLs-VrIBqaB_FXkJJ_KdNs1JW0iikIEdrsl5ZwosXF8",
                    dinersCount = 4,
                    likes = 124,
                    comments = 18,
                    caption = "Finally checked out Obama's favorite spot! The smoky pork is unmatched. 🍜✨",
                    shareAmount = "85.000 ₫",
                    onSettleUp = { onSettleUp("bill_1") }
                )
            }

            item {
                // Feed Card 2: Asymmetric Layout
                AsymmetricSplitCard(
                    image = "https://lh3.googleusercontent.com/aida-public/AB6AXuAj9XK9B0XwL6NiRTf3VdPHqESts0TT-GmGiMxMa4FTS_lFBTxyiYDh9BCJQhbFMU9ZtptxHJsp91d37oG9c_8ovmXnCyURNp1iwca1H-6pWLmxeoqNw3yE9v85y0SRnE-T-_1A9ekQ8oq1XO52ON8Mih6a8urFZppTZrtp88qHj95Ou0ymxg58hJBRwhsJk05PpFL2y14KcH64XJtBwHZPCi19rA6nAbVTXgpIi9nRRF3vhOz5-h0_auOA8yKy9Lyr2XA5LepGjzw",
                    userName = "An Trần",
                    userAvatar = "https://lh3.googleusercontent.com/aida-public/AB6AXuBSbMAuuUDIimrLxCbv23Ulz0K4Mufh7SmxZWxCDgVKbMUUDv28qxOHAOcolcVL1383bKxSGj1k_hiU1gBAtun1oQB8q2dG5GVWoOGYkr9ugpwrUQq3v9VPJ6xLR3H6eiV38V5_DJto-1Mk43CmVZYmByzKYj6TNOk0xuva9tJDZ77z2WZ7hYSRLZh0Pcxc5YZ4XKpA793nqBoIEU_muAFKAiY6sJXz46IzIcbJOLHvIpWVdsBa9i37qtOyLihPyhbPDRGVwR4or8c",
                    title = "Weekend Dim Sum at Chợ Lớn",
                    quote = "Best shrimp dumplings in District 5.",
                    totalBill = "1.240.000 ₫",
                    timeAgo = "2H AGO",
                    participantsAvatars = listOf(
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuD8PGjMT8_k2pIk0pNhPlZyn95_UaNPVme5g4RRyDL65owDT3X4RgLwFMiKsV0KpmDBp9prt9W5NuZgIshHFxcWeW4gZ2a6XaVAIRLAzq7lEWa_-W83ypVHDuMW0-w04F9Xp8cN7TfJRnQwQahRW7y-Y9OBTcln61z943LhnZtdMGD3DU7PvHuFr25oAQsssO6eYD43H8C-Zp2-ikrk23HM7Kbe95CNPfFWjOzQ-7Fw4rRgFLl-CMI_v_DcrfjIR9X5gxtpogvJlRw",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuDAMaMSVkZY1n0pCd-1dM7QExKadcAMxlbrJZ9vvzaUqPAWEb6X164HPG2k_yi-9xSoC0MGIiZnvgU0YTWriXF8fNKo7zTdu8Z8-S6p3aRxMAWGLoZiWPNJos31SEqLHSPeZtru5FjaLN9thAnty8_o6FPR1Aj645XdNpuv4TArYOPbRMTVzQPBzJkdQf0VgKY1QE1C9cl4u1ED2zSm0H0oFqpX1pt62tkjY8Pl4U7Ydd7l1PyQdfuO91HqJnd57-Bcorp-9r6urvg",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuDE1N5a32jARp5WKVmPlLPtF3vYXXet7x0PcDxLt6tzVv4otuztefwepxQu76Rnli9zGjI52x7Co017qFdxuDEjnHUsC5i9h6j_1cIwEDLWSx8ErOabo4N2RZctC0HJphqa2axTJeCaaRFyd_IdQItFwGNWRH2Xu6f313gMboETg6EkpgLFiIBCg_WB0_IugteaqCTq9Fp9RGMH6CfwszDf7zsV2M4wHCI8hr_7lJqD7ou3htpy9C7Mybby-uV6yNZC7hmB9YfQygQ"
                    ),
                    extraParticipants = 2
                )
            }

            item {
                // Feed Card 3: Large Editorial Image
                EditorialMomentCard(
                    image = "https://lh3.googleusercontent.com/aida-public/AB6AXuCGV9c01v4w81VRtAWFcQUHRBEziHthGF_vOyP3Tv6qCvE9pKkUTjCG8tza6TIwRalxo4Q39RK5w3NUB8FFeyQ0cE-qcBVWYPjcwjOiO3yBgr4OW2NY5kMmgoW8T2X1vDeQdosHnfv6IgzLCb2I4lyxbc878QsQ3be9HDCH2sjsob3-Y_rUncWD0RV4HlzFyyEYIHv3JBIVHsdJfhaag7LVOzDq7XcMwSJtwdidm4J5d7E6_TGbUw2u6nW1nt8YNYql_YJLCLvAFv4",
                    title = "Midnight Snacking in Sài Gòn",
                    status = "HAPPENING NOW"
                )
            }
        }
    }
}

@Composable
private fun FeedTopBar(userAvatarUrl: String?, onOpenNotifications: () -> Unit, onOpenSearch: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = userAvatarUrl.takeIf { !it.isNullOrBlank() } ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuCpnvw7NZghOMJRnH_WaC3eHutJm9XoZmybu_TS_uk9WGcWsJ_ROjtI90_bvBZh8RdgNB0TqYRJz9rZwQs8ccGh0XZdffsyr3NPpk2NVubfaS48U6sqwA-G3_MDzJUaOs2ZwR38m4yLqhn5qc9roHjyOG9DRe0snpGmqEaalIMhGPfnnWqyYIKfjwhLix41mqjPZc3XGCjrn-j-XR7ybsOoJqCMtxUDerTeRQZEdpJI07YwJUZ1l4qlO-YwxfFb6oJMhnd9dTple0c",
                        contentDescription = "My Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = "DineSplit",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onOpenNotifications) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentGroupVibes() {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = "RECENT GROUP VIBES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Current Meal Status
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(176.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDK-tvPMo867EAwEwleBgzX8R9QReUBHS9zRKDNRQeVBoRqUOW5HiZEk7lP__cGXdsQk54h1RXzZsyYX6PNbnXnIUEq8qInVBjCxseD_Xni_-dzFLT3Lnb2LuZQNOYPNU8PPqmlmeN2wvNi3UF4CfefCWdXID6R0lRwkQtzJai3znOl7lStckvTq7Z1ZELG1yjE0zCZ7AKAIS0v1uedtPu-pkZtUho7J-6NTfgh_WIkjME8KvLwICokqGXbq48s2prybOjR-ASSHRM",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().alpha(0.6f),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                    
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    ) {
                        Text("YOU'RE EATING", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color.White.copy(alpha = 0.8f))
                        Text("Phở Gia Truyền", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }
            
            items(vibes) { vibe ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                if (vibe.hasStory) Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary))
                                else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh)),
                                CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        AsyncImage(
                            model = vibe.avatar,
                            contentDescription = vibe.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(vibe.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun SocialSplitCard(
    userAvatar: String,
    userName: String,
    location: String,
    mainImage: String,
    dinersCount: Int,
    likes: Int,
    comments: Int,
    caption: String,
    shareAmount: String,
    onSettleUp: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(
                        model = userAvatar,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(userName, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(location, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = MaterialTheme.colorScheme.outline)
                }
            }

            // Main Image
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .aspectRatio(0.8f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = mainImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = CircleShape,
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("$dinersCount DINERS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black))
                    }
                }
            }

            // Stats
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(likes.toString(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(comments.toString(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(20.dp))
            }

            // Caption
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$userName ") }
                    append(caption)
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Split Bill Section
            Surface(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("YOUR SHARE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.outline)
                        Text(shareAmount, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = onSettleUp,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)))
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text("SETTLE UP", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AsymmetricSplitCard(
    image: String,
    userName: String,
    userAvatar: String,
    title: String,
    quote: String,
    totalBill: String,
    timeAgo: String,
    participantsAvatars: List<String>,
    extraParticipants: Int
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Image Half
            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                AsyncImage(model = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(model = userAvatar, contentDescription = null, modifier = Modifier.size(24.dp).clip(CircleShape).border(1.dp, Color.White, CircleShape))
                    Text(userName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }

            // Info Half
            Column(modifier = Modifier.weight(1.3f).padding(16.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                            Text("SETTLED", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Text(timeAgo, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("\"$quote\"", style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        participantsAvatars.forEach { url ->
                            AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(32.dp).border(2.dp, Color.White, CircleShape).clip(CircleShape), contentScale = ContentScale.Crop)
                        }
                        if (extraParticipants > 0) {
                            Box(modifier = Modifier.size(32.dp).border(2.dp, Color.White, CircleShape).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                                Text("+$extraParticipants", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Bill", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(totalBill, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialMomentCard(image: String, title: String, status: String) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .aspectRatio(1.77f)
            .clip(RoundedCornerShape(24.dp))
    ) {
        AsyncImage(model = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.9f)))))
        
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(status, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = Color.White.copy(alpha = 0.6f))
                Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

data class Vibe(val name: String, val avatar: String, val hasStory: Boolean)
private val vibes = listOf(
    Vibe("Minh Tú", "https://lh3.googleusercontent.com/aida-public/AB6AXuB-lcBKRoAYwQw73nIuBdULF7SZEeFfg2TOaffudPtrcOnyx_8_a249_LJcMx5TBluLjiWE8fbEcy4eV7gK7RbihQblIjmgOP5B7c55rKXy9JsKjJjQmetVj5yL0q9GvyYQPiR0_ZnmJv_VjBFl-eNXvTyxNV_wGEHIOerHgr7-Bhr0JQ52rl2IHVEA925v4ju8vhX_A3TbdL37vEXq2FZ6hzChgpASZ9lmR1dkpJprauIMSfg-jAAb0dHPuAtCHgI4cY8VOV-Agd4", true),
    Vibe("Khánh Linh", "https://lh3.googleusercontent.com/aida-public/AB6AXuBz8Jc-E35SQpfts5F-5Eczw6dWoYmC-HCNwwt8GI_w0EWLE-2FnqQ8mgZvohpGRKnOAVGodaj82NSuuH_X44mCJJF7svdrXs69vjYwM96R4FUn5f4TKPG3hUkyjfKZH4SiY7gfmVzYcX-w6uDBdpBiMt_ZPYvDEIlUp5JJt-Wworohv65EZUi3d15JqXw6myxzpL87IYhIB4EmDZooMn6Y3D8DcEbET8nOa6KpvmgNiVWOgGg3Cd0oMcbuAHlEdpFlt0R-injRfPo", false),
    Vibe("Thế Huy", "https://lh3.googleusercontent.com/aida-public/AB6AXuDgF0M5FazB2IT4juh4tcOt4K1Ebn3YjSLLXXnEO_orZuRvR7754qsoNDOrLZZRk9MBdEyyJm92iSBTTnUo254hKU062XQiAI0pDu2ZzQ6qUeeRLIRs31LkLGwZlpQVMko9-vOn8jdvYQxhY1IXcHNASxdE5qHGU8nV6uM1v89Ykoyi-NsBff_wlPgG-H-Xsclt1CrCt3PDOJlWGuMnbGFCAkt3p8c5XbDj3XqELFVIf12Tnm9BMHwVXvqOCJPx3F_X1-e8Nyuo7RU", false)
)

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        FeedScreen(
            userAvatarUrl = null,
            onOpenNotifications = {},
            onOpenSearch = {},
            onSettleUp = {}
        )
    }
}
