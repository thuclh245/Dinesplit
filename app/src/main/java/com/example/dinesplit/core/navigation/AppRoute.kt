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
    data object AddTransaction : AppRoute("add_transaction") {
        const val ARG_TYPE = "type"
        val routeWithArg = "$route?$ARG_TYPE={$ARG_TYPE}"

        fun createRoute(type: String? = null): String {
            if (type.isNullOrBlank()) return route
            return "$route?$ARG_TYPE=$type"
        }
    }
    data object Profile : AppRoute("profile")

    data object Notifications : AppRoute("notifications")
    data object Assistant : AppRoute("assistant")
}
