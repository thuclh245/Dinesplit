package com.example.dinesplit.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL
import kotlin.io.use

@Composable
fun ProfileAvatarSection(
    avatarModel: Any?,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    clearText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    isBusy: Boolean = false,
) {
    val avatarBitmap by rememberAvatarBitmap(avatarModel)

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Box(
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap!!.asImageBitmap(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!actionText.isNullOrBlank() && onActionClick != null) {
                PrimaryButton(
                    text = if (isBusy) "$actionText..." else actionText,
                    onClick = onActionClick,
                    enabled = !isBusy,
                )
            }

            if (!clearText.isNullOrBlank() && onClearClick != null) {
                SecondaryButton(
                    text = clearText,
                    onClick = onClearClick,
                    enabled = !isBusy,
                )
            }
        }
    }
}

@Composable
private fun rememberAvatarBitmap(avatarModel: Any?): androidx.compose.runtime.State<Bitmap?> {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, avatarModel) {
        value = loadAvatarBitmap(context, avatarModel)
    }
}

private suspend fun loadAvatarBitmap(
    context: Context,
    avatarModel: Any?,
): Bitmap? {
    val model = avatarModel?.toString().orEmpty()
    if (model.isBlank()) return null

    return withContext(Dispatchers.IO) {
        runCatching {
            val uri = model.toUri()
            when (uri.scheme) {
                "content", "file", "android.resource" -> context.contentResolver.openInputStream(uri).useBitmapStream()
                "http", "https" -> URL(model).openStream().useBitmapStream()
                else -> context.contentResolver.openInputStream(uri).useBitmapStream() ?: URL(model).openStream().useBitmapStream()
            }
        }.getOrNull()
    }
}

private fun InputStream?.useBitmapStream(): Bitmap? {
    this ?: return null
    return use { stream -> BitmapFactory.decodeStream(stream) }
}
