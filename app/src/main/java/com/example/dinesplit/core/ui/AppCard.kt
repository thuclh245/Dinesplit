package com.example.dinesplit.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AppDimens.spaceLg),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = AppShapes.xLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = AppDimens.cardElevation,
            ),
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun OutlinedAppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AppDimens.spaceLg),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = AppShapes.xLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = AppDimens.level0,
            ),
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun ElevatedAppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AppDimens.spaceLg),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = AppShapes.xLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = AppDimens.level2,
            ),
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun ClickableAppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(AppDimens.spaceLg),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .clip(AppShapes.xLarge)
            .clickable(enabled = enabled, onClick = onClick),
        shape = AppShapes.xLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = AppDimens.cardElevation,
            ),
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
