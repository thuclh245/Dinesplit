package com.example.dinesplit.presentation.feed

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dinesplit.ui.theme.DineSplitTheme

@Composable
fun SearchScreen(
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SearchTopBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Recent Searches
            item {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    SectionHeader("Recent Searches")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(listOf("Pizza", "Sushi", "Minh Tu", "Tacos")) { search ->
                            RecentSearchChip(text = search)
                        }
                    }
                }
            }

            // Discover People
            item {
                Column {
                    SectionHeader("Discover People")
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PersonCard(
                            name = "Alex Rivera",
                            username = "@arivera",
                            avatar = "https://lh3.googleusercontent.com/aida-public/AB6AXuDwxJnHovEA4MZZn9_eD7i7DfH26b0g9SmrcAMRkO3PQvhUxRiuLYUSwSdrzF5tI15Tf5_QjCIsK3-zmmXq85-hjWR7JDv0nL46Gxzjtol8bTEDtcNtce1HTbUzG9Tl0B7IUz8v582Q8bnm3-QeQE8kR2c3b0sjBTj0qbAHIie7WPaU1c3Rak4lgJYQEkvUgX69rYbBO2cHdasU-aUybsRDmgAz4buZSO10y2Y_c5JiQdFD8386aat4eQnqfF9AZRBy3KNm4h31sQ"
                        )
                        PersonCard(
                            name = "Sarah Chen",
                            username = "@schen_eats",
                            avatar = "https://lh3.googleusercontent.com/aida-public/AB6AXuADofLptzzae-aLxMe7sdKsfW_WuKzC2SA61NgiVHocSdFixcvw3Z-zOXpYMATh-994KFD43AJIKO60mpimtqsxdh2rBKrwhpyvL62G9vfr4qV7c0nVCgSRa0CIFM-dY62ELfBq-5ssnWNecSo6fZF8_3Z-2izT1eoEhsdEym5ZRnJtQ1p5vFh4GF8d4ts0MHERtAd8Rv6XjOseuOUk5tE_hxI7E6evpiPfPE_11sJSh0AsQx6a6Ur9Llm8fXWNiflnQYzhJlZzJg"
                        )
                    }
                }
            }

            // Trending Places
            item {
                Column {
                    SectionHeader("Trending Places")
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        PlaceCard(
                            name = "The Rustic Spoons",
                            category = "Artisanal Italian",
                            rating = "4.8",
                            distance = "1.2 mi",
                            priceRange = "$$$",
                            image = "https://lh3.googleusercontent.com/aida-public/AB6AXuAnKvQKCCduaFahO55imH8Cl_EDOrKzD3axmpQl65HQdBlT-AvIAYxjxe-Iq2cLOXN_QBN51DrnEjhtaPAWPe2pT7QCSXolQ6eQIQfn0KozsJ7NprB-f8mJStrrAmYHt6Ifz9821HotOldGO8chnQ9MDmdEPqu4pFmpzKUh1zLllrrHhJIBDC0hTE8erOZBXMsqYcqXhnycvovS241S6TCDAym__w04HbAcz2mnktnxKVfuWgKkmgy2lSRvRh6lHSLGA17PyB6W-Q"
                        )
                        PlaceCard(
                            name = "Urban Greens",
                            category = "Healthy Bowls",
                            rating = "4.6",
                            distance = "0.8 mi",
                            priceRange = "$$",
                            image = "https://lh3.googleusercontent.com/aida-public/AB6AXuCkxScXIbLdu6IiOy23I0nbl2-tb8Zhwv5q6IOHlBqhaI2piPVrmIp0iMOuGCPZfmQDg5OkvifWKNUMCEKZ0h0a09Qa-OdIzFKMjHGqjbOv_ri4hEO1W_ofZ6RZxhjH2ey-gZ8jBqTOc1ErG-cGKZPNsxALDyFAM6xHYP_SYCrz-7gdSTyMWUv50ARCoL_Bvlfg9uwEHrWaKpCgHqR7QGFApntACPfJMHNoW1Q1PGfbIZyxPiQs2p1ksTVn6_v_uIGlBbDuhvVBpQ"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                placeholder = { Text("Search...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outlineVariant) },
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
    )
}

@Composable
private fun RecentSearchChip(text: String) {
    Surface(
        onClick = { },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outlineVariant)
            Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun PersonCard(name: String, username: String, avatar: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AsyncImage(
                    model = avatar,
                    contentDescription = name,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
                    contentScale = ContentScale.Crop
                )
                Column {
                    Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Text(username, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(
                onClick = { },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Follow", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun PlaceCard(
    name: String,
    category: String,
    rating: String,
    distance: String,
    priceRange: String,
    image: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                AsyncImage(model = image, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.1f)))
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Text(rating, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("$category • $priceRange • $distance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        SearchScreen(onBack = {})
    }
}
