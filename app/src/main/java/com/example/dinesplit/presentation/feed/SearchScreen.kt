package com.example.dinesplit.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField

private data class SearchSuggestion(
    val title: String,
    val subtitle: String
)

@Composable
fun SearchScreen(
    onBack: () -> Unit = {}
) {
    var keyword by remember { mutableStateOf("") }
    val suggestions = listOf(
        SearchSuggestion("Bep Nha Xua", "Restaurant"),
        SearchSuggestion("Pho 24", "Restaurant"),
        SearchSuggestion("@linh.foodie", "User"),
        SearchSuggestion("@team.weekend", "Group")
    )
    val filteredSuggestions = suggestions.filter {
        keyword.isBlank() || it.title.contains(keyword, ignoreCase = true)
    }

    AppScaffold(
        title = "Search",
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            AppTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = "Search",
                placeholder = "Find users or restaurants"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                items(filteredSuggestions) { item ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(AppDimens.spaceMd)
                            ) {
                                Text(
                                    text = item.title.take(1),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
