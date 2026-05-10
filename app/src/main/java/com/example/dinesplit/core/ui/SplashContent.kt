package com.example.dinesplit.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.dinesplit.ui.theme.DineSplitTheme

@Composable
fun SplashContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        val secondaryGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBlob(Offset(size.width * 0.88f, size.height * 0.10f), size.minDimension * 0.38f, primaryGlow)
            drawBlob(Offset(size.width * 0.10f, size.height * 0.86f), size.minDimension * 0.34f, secondaryGlow)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalDining,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Dine",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp, fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Split",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp, fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth(0.14f)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50))
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Eat together, split effortlessly.",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "THE SOCIAL LEDGER FOR FOODIES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 2.sp
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ShimmerLoadingBar(
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .height(6.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarBubble(label = "A", offsetDp = 0.dp)
                    AvatarBubble(label = "M", offsetDp = (-12).dp)
                    AvatarBubble(label = "K", offsetDp = (-12).dp)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Joining 24k+ food lovers",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        FloatingChip(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 72.dp, start = 4.dp),
            icon = Icons.Filled.Payments,
            tint = MaterialTheme.colorScheme.secondary,
            rotation = -12f
        )
        FloatingChip(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 4.dp),
            icon = Icons.Filled.EmojiEvents,
            tint = MaterialTheme.colorScheme.primary,
            rotation = 15f
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashContentPreview() {
    DineSplitTheme {
        SplashContent()
    }
}

private fun DrawScope.drawBlob(centerOffset: Offset, radius: Float, color: Color) {
    drawCircle(color = color, radius = radius, center = centerOffset)
}

@Composable
private fun ShimmerLoadingBar(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "splash-shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash-loading-alpha"
    )

    Box(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(50)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun AvatarBubble(label: String, offsetDp: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .offset(x = offsetDp)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FloatingChip(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    rotation: Float
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(22.dp)
        )
    }
}









