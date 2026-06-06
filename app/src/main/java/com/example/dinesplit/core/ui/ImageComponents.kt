package com.example.dinesplit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest

/**
 * Avatar ảnh tròn có đầy đủ fallback:
 * - URL null/trống → hiển thị chữ cái đầu của [name] (hoặc icon Person nếu name cũng null)
 * - URL lỗi tải → fallback tương tự chữ cái đầu
 */
@Composable
fun DineAvatarImage(
    imageUrl: String?,
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fallbackContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    fallbackContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val initial = name?.trim()?.firstOrNull()?.uppercase()
    var isError by remember(imageUrl) { mutableStateOf(false) }
    val initialStyle = if (size <= 32.dp) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.titleMedium
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fallbackContainerColor),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank() && !isError) {
            val context = LocalContext.current
            val request = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    isError = state is AsyncImagePainter.State.Error
                },
            )
        } else if (!initial.isNullOrBlank()) {
            Text(
                text = initial,
                style = initialStyle,
                fontWeight = FontWeight.ExtraBold,
                color = fallbackContentColor
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = fallbackContentColor,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/**
 * Ảnh bài viết hình chữ nhật có placeholder shimmer và error fallback icon.
 */
@Composable
fun DinePostImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(AppDimens.radiusLg),
) {
    var isError by remember(imageUrl) { mutableStateOf(false) }
    var isLoading by remember(imageUrl) { mutableStateOf(true) }

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank() && !isError) {
            val context = LocalContext.current
            val request = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    isLoading = state is AsyncImagePainter.State.Loading
                    isError = state is AsyncImagePainter.State.Error
                },
            )
        } else {
            // Error or null URL
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

/**
 * Ảnh dạng grid vuông, dùng cho PhotoGrid trong ProfileScreen.
 */
@Composable
fun DineGridImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(AppDimens.radiusSm),
) {
    DinePostImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        shape = shape,
    )
}
