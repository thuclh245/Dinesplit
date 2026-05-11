package com.example.dinesplit.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Feed : BottomTab(
        route = AppRoute.Feed.route,
        label = "Feed",
        icon = Icons.AutoMirrored.Outlined.ReceiptLong
    )

    data object Split : BottomTab(
        route = AppRoute.Split.route,
        label = "Split",
        icon = Icons.AutoMirrored.Outlined.CallSplit
    )

    data object Personal : BottomTab(
        route = AppRoute.Personal.route,
        label = "Personal",
        icon = Icons.Outlined.AccountBalanceWallet
    )

    data object Profile : BottomTab(
        route = AppRoute.Profile.route,
        label = "Profile",
        icon = Icons.Outlined.Person
    )

    companion object {
        val items = listOf(Feed, Split, Personal, Profile)
    }
}