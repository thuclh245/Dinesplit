package com.example.dinesplit.core.navigation

sealed class AppRoute(val route: String) {

    data object Splash : AppRoute("splash")
    data object Login : AppRoute("login")
    data object Register : AppRoute("register")
    data object CompleteProfile : AppRoute("complete_profile")

    data object MainContainer : AppRoute("main")

    data object Feed : AppRoute("feed")
    data object Split : AppRoute("split")
    data object Personal : AppRoute("personal")
    data object Profile : AppRoute("profile")

    data object Notifications : AppRoute("notifications")
    data object Assistant : AppRoute("assistant")
}
