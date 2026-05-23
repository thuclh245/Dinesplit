package com.example.dinesplit.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.dinesplit.domain.model.AppStartDestination
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationType
import com.example.dinesplit.presentation.assistant.AssistantScreen
import com.example.dinesplit.presentation.auth.CompleteProfileScreen
import com.example.dinesplit.presentation.auth.LoginScreen
import com.example.dinesplit.presentation.auth.RegisterScreen
import com.example.dinesplit.presentation.auth.SplashScreen
import com.example.dinesplit.presentation.main.MainContainerScreen
import com.example.dinesplit.presentation.notification.NotificationScreen

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash.route
    ) {
        composable(AppRoute.Splash.route) {
            SplashScreen(
                onDestinationResolved = { destination ->
                    when (destination) {
                        AppStartDestination.AUTH -> navController.navigate(NavGraph.AUTH) {
                            popUpTo(AppRoute.Splash.route) { inclusive = true }
                        }
                        AppStartDestination.COMPLETE_PROFILE -> navController.navigate(AppRoute.CompleteProfile.route) {
                            popUpTo(AppRoute.Splash.route) { inclusive = true }
                        }
                        AppStartDestination.MAIN -> navController.navigate(NavGraph.MAIN) {
                            popUpTo(AppRoute.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        navigation(
            route = NavGraph.AUTH,
            startDestination = AppRoute.Login.route
        ) {
            composable(AppRoute.Login.route) {
                LoginScreen(
                    onGoToRegister = {
                        navController.navigate(AppRoute.Register.route)
                    },
                    onLoginSuccess = { destination ->
                        val target = when (destination) {
                            AppStartDestination.MAIN -> NavGraph.MAIN
                            AppStartDestination.COMPLETE_PROFILE -> AppRoute.CompleteProfile.route
                            AppStartDestination.AUTH -> AppRoute.Login.route
                        }

                        if (target != AppRoute.Login.route) {
                            navController.navigate(target) {
                                popUpTo(NavGraph.AUTH) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(AppRoute.Register.route) {
                RegisterScreen(
                    onGoToLogin = {
                        navController.navigateUp()
                    },
                    onRegisterSuccess = {
                        navController.navigate(AppRoute.CompleteProfile.route)
                    }
                )
            }

            composable(AppRoute.CompleteProfile.route) {
                CompleteProfileScreen(
                    onCompleteProfileSuccess = {
                        navController.navigate(NavGraph.MAIN) {
                            popUpTo(NavGraph.AUTH) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        navigation(
            route = NavGraph.MAIN,
            startDestination = AppRoute.MainContainer.route
        ) {
            composable(
                route = AppRoute.MainContainer.routeWithArgs,
                arguments = listOf(
                    navArgument(AppRoute.MainContainer.ARG_TAB) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(AppRoute.MainContainer.ARG_TARGET) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val initialTabRoute = backStackEntry.arguments
                    ?.getString(AppRoute.MainContainer.ARG_TAB)
                    ?.let(Uri::decode)
                    .orEmpty()
                    .ifBlank { AppRoute.Feed.route }
                val pendingRoute = backStackEntry.arguments
                    ?.getString(AppRoute.MainContainer.ARG_TARGET)
                    ?.let(Uri::decode)
                    ?.takeIf { it.isNotBlank() }

                MainContainerScreen(
                    initialTabRoute = initialTabRoute,
                    pendingRoute = pendingRoute,
                    onOpenNotifications = {
                        navController.navigate(AppRoute.Notifications.route)
                    },
                    onOpenAssistant = {
                        navController.navigate(AppRoute.Assistant.route)
                    },
                    onLogout = {
                        navController.navigate(NavGraph.AUTH) {
                            popUpTo(NavGraph.MAIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(AppRoute.Notifications.route) {
            NotificationScreen(
                onNotificationClick = { notification ->
                    navController.navigate(notification.toMainContainerRoute()) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppRoute.Assistant.route) {
            AssistantScreen()
        }
    }
}

private fun Notification.toMainContainerRoute(): String {
    val destination = deepLinkDestination
    val targetId = deepLinkTargetId

    return when {
        // Activity/Feed detail
        destination == "ACTIVITY_DETAIL" && !targetId.isNullOrBlank() -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Feed.route,
                target = AppRoute.PostDetail.createRoute(targetId)
            )
        }
        // Personal transaction detail
        destination == "TRANSACTION_DETAIL" && !targetId.isNullOrBlank() -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Personal.route,
                target = AppRoute.TransactionDetail.createRoute(targetId)
            )
        }
        // Spending reminders (budget alert)
        destination == "SPENDING_REMINDERS" -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Personal.route,
                target = AppRoute.SpendingReminders.route
            )
        }
        // Split bill detail
        destination == "SPLIT_DETAIL" && !targetId.isNullOrBlank() -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Split.route
            )
        }
        // Split settle/payment
        destination == "SPLIT_SETTLE" && !targetId.isNullOrBlank() -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Split.route
            )
        }
        // Other user profile (from activity)
        destination == "PROFILE" && !targetId.isNullOrBlank() -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Profile.route,
                target = AppRoute.OtherUserProfile.createRoute(targetId)
            )
        }
        // Fallback: route to split tab if payment/bill notification
        type in setOf(
            NotificationType.BILL_CREATED,
            NotificationType.PAYMENT_PENDING,
            NotificationType.PAYMENT_COMPLETED,
            NotificationType.SPLIT_COMPLETED
        ) -> {
            AppRoute.MainContainer.createRoute(tab = AppRoute.Split.route)
        }
        // Fallback: route to activity/feed if activity update
        type == NotificationType.ACTIVITY_UPDATE -> {
            AppRoute.MainContainer.createRoute(tab = AppRoute.Feed.route)
        }
        // Fallback: route to personal if transaction alert
        type == NotificationType.TRANSACTION_ALERT -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Personal.route,
                target = AppRoute.SpendingReminders.route
            )
        }
        // Default fallback
        else -> AppRoute.MainContainer.createRoute()
    }
}
