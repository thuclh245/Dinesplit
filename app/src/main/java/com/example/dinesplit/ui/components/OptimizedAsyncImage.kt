package com.example.dinesplit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Memory-optimized AsyncImage that handles loading states and errors gracefully
 */
@Composable
fun OptimizedAsyncImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    clipShape: RoundedCornerShape? = null,
) {
    if (model.isNullOrEmpty()) {
        Box(
            modifier =
                modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .let { if (clipShape != null) it.clip(clipShape) else it },
        )
        return
    }

    Box(
        modifier = modifier.let { if (clipShape != null) it.clip(clipShape) else it },
    ) {
        AsyncImage(
            model =
                ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .crossfade(true)
                    .build(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
        )
    }
}
