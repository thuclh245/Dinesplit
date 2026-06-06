package com.example.dinesplit.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage

@Composable
fun SplashContent(progress: Float = 0f) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        // --- Background Editorial Texture ---
        // Top right glow - using radial gradient to prevent hard rectangular edge artifacts and ensure soft fallback
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-100).dp)
                    .size(300.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    ),
        )

        // Bottom left glow - using radial gradient to prevent hard rectangular edge artifacts and ensure soft fallback
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-80).dp, y = 80.dp)
                    .size(280.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    ),
        )

        // --- Floating Elements ---
        Box(
            modifier =
                Modifier
                    .padding(top = 200.dp, start = 40.dp)
                    .alpha(0.4f)
                    .rotate(-12f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp),
            )
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 300.dp, end = 32.dp)
                    .alpha(0.4f)
                    .rotate(15f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        // --- Main Content ---
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 96.dp, horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Section: Brand Icon
            Box(contentAlignment = Alignment.Center) {
                // Ambient Glow - using radial gradient to prevent square layout outline/clipping artifacts
                Box(
                    modifier =
                        Modifier
                            .size(120.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            ),
                )

                Box(
                    modifier =
                        Modifier
                            .size(96.dp)
                            .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary,
                                    ),
                                ),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            // Middle Section: Brand Identity
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text =
                            buildAnnotatedString {
                                append("Dine")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("Split")
                                }
                            },
                        style =
                            MaterialTheme.typography.displayLarge.copy(
                                fontSize = 60.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-2).sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier =
                            Modifier
                                .width(48.dp)
                                .height(2.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text =
                            buildAnnotatedString {
                                append("Eat together, split ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary, fontStyle = FontStyle.Italic)) {
                                    append("effortlessly")
                                }
                                append(".")
                            },
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 28.sp,
                            ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = "THE SOCIAL LEDGER FOR FOODIES",
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 2.sp,
                            ),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            // Progress Loading Indicator
            Box(
                modifier =
                    Modifier
                        .width(192.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
                    label = "progress_animation",
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                ),
                            ),
                )
            }

            // Social Hint / Trust Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy((-12).dp),
                ) {
                    UserAvatar(
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuBVrs2wx5anloIhdMmQnabHmpo0H_8ln0beyIY9S0ro1i1jJHKS56WqaxeXOL7euxP3gZKUxfRDX7HXISYMpYFDXNTuC6qB7GM3H89l03EasLEIrsQvTU3jqQl5WpSG9KzlOjNx9XKsx8cgq-Ji-ORq_PwZewyVk2S-CVe4mkptTFUctRA8kjxbpe8QP19-Pgxe8vP9NHU1c3-n1LQFME8a6bXb0RYnWF0PYWZGUn1f-fRHhOwOdiHvAD-6a2SVLEG1bYHvEJOzMUo",
                    )
                    UserAvatar(
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuBWMGCgdIuILTkYdpwu6NFuMAYl3jQ6J5w6zSGMLMCAX4jObRTGxmtX--hl_Em-gXXyBEjoqEM8nhIlRkjHUsaoe1QS3BNmK4C3CNRKcZptBpMbhgyTVLKDsm2BpxXtoHqDuqVW9OF6-4_oS8TL72GRR0t_r9V_uSwGOJbsVD83wxb0ktNI2D3dT2W39BjjjbO9tePkLxU8ZmAu3RChYz-0bXxRnlcN9_PrbFD3mvLF66gtH0BKRzBxDRM39q7sZ-PnRgv37Dsdek4",
                    )
                    UserAvatar(
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuB-dgw3J2EUB-AGRgIB2YgfLLReweeQaL6U5HIkTr7eoW8E4fdbnGMj5Fq2apIEfkz5fQN2te9Gpr3eusWb5XYaD8L4OK6ZNDx_6sMLbgKT18oex2iAOMU5aD3ZwMZlZqX41TaaB9e4fR1p40vpFfTUbEM_xqfOQ45WaQ-78s94spulAMV-HEz7PAJpMoDLheZwBoxECEB-wH-WumRqROqcnDDLc-idXRZQWdcOqGrOo4W9nCZ6Ox32uq66NMjQeBEnTWbWU7ihDhM",
                    )
                }
                Text(
                    text = "JOINING 24K+ FOOD LOVERS",
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun UserAvatar(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier =
            Modifier
                .size(40.dp)
                .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        contentScale = ContentScale.Crop,
    )
}

@Preview(showBackground = true)
@Composable
fun SplashContentPreview() {
    MaterialTheme {
        SplashContent(progress = 0.5f)
    }
}
