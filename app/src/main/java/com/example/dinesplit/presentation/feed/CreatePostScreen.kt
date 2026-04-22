package com.example.dinesplit.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.example.dinesplit.core.ui.AppButton
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField

@Composable
fun CreatePostScreen(
    onBack: () -> Unit = {}
) {
    var restaurantName by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }

    AppScaffold(
        title = "Create Post",
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            AppCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap to select food photo",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            AppTextField(
                value = restaurantName,
                onValueChange = { restaurantName = it },
                label = "Restaurant",
                placeholder = "Ex: Bep Nha Xua"
            )

            AppTextField(
                value = caption,
                onValueChange = { caption = it },
                label = "Caption",
                placeholder = "How did your meal go?",
                singleLine = false
            )

            Spacer(modifier = Modifier.height(AppDimens.spaceSm))

            AppButton(
                text = "Post",
                onClick = {},
                enabled = restaurantName.isNotBlank() && caption.isNotBlank()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}
