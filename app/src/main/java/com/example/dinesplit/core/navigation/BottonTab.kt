package com.example.dinesplit.core.navigation

sealed class BottomTab(
    val route: String,
    val label: String
) {
    data object Feed : BottomTab(
        route = AppRoute.Feed.route,
        label = "Feed"
    )

    data object Split : BottomTab(
        route = AppRoute.Split.route,
        label = "Split"
    )

    data object Personal : BottomTab(
        route = AppRoute.Personal.route,
        label = "Personal"
    )

    data object Profile : BottomTab(
        route = AppRoute.Profile.route,
        label = "Profile"
    )

    companion object {
        val items = listOf(Feed, Split, Personal, Profile)
    }
}