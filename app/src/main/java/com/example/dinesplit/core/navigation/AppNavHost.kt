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
import com.example.dinesplit.domain.model.NotificationDestination
import com.example.dinesplit.domain.model.NotificationType
import com.example.dinesplit.presentation.assistant.AssistantScreen
import com.example.dinesplit.presentation.auth.CompleteProfileScreen
import com.example.dinesplit.presentation.auth.LoginScreen
import com.example.dinesplit.presentation.auth.RegisterScreen
import com.example.dinesplit.presentation.auth.SplashScreen
import com.example.dinesplit.presentation.main.MainContainerScreen
import com.example.dinesplit.presentation.notification.NotificationScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash.route,
    ) {
        composable(AppRoute.Splash.route) {
            SplashScreen(
                onDestinationResolved = { destination ->
                    when (destination) {
                        AppStartDestination.AUTH ->
                            navController.navigate(NavGraph.AUTH) {
                                popUpTo(AppRoute.Splash.route) { inclusive = true }
                            }
                        AppStartDestination.COMPLETE_PROFILE ->
                            navController.navigate(AppRoute.CompleteProfile.route) {
                                popUpTo(AppRoute.Splash.route) { inclusive = true }
                            }
                        AppStartDestination.MAIN ->
                            navController.navigate(NavGraph.MAIN) {
                                popUpTo(AppRoute.Splash.route) { inclusive = true }
                            }
                    }
                },
            )
        }

        navigation(
            route = NavGraph.AUTH,
            startDestination = AppRoute.Login.route,
        ) {
            composable(AppRoute.Login.route) {
                LoginScreen(
                    onGoToRegister = {
                        navController.navigate(AppRoute.Register.route)
                    },
                    onLoginSuccess = { destination ->
                        val target =
                            when (destination) {
                                AppStartDestination.MAIN -> NavGraph.MAIN
                                AppStartDestination.COMPLETE_PROFILE -> AppRoute.CompleteProfile.route
                                AppStartDestination.AUTH -> AppRoute.Login.route
                            }

                        if (target != AppRoute.Login.route) {
                            navController.navigate(target) {
                                if (target == NavGraph.MAIN) {
                                    popUpTo(NavGraph.AUTH) { inclusive = true }
                                }
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }

            composable(AppRoute.Register.route) {
                RegisterScreen(
                    onGoToLogin = {
                        navController.navigateUp()
                    },
                    onRegisterSuccess = { displayName ->
                        navController.navigate(AppRoute.CompleteProfile.createRoute(displayName))
                    },
                )
            }
        }

        composable(
            route = AppRoute.CompleteProfile.routeWithArg,
            arguments = listOf(
                navArgument(AppRoute.CompleteProfile.ARG_DISPLAY_NAME) {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val displayName = backStackEntry.arguments?.getString(AppRoute.CompleteProfile.ARG_DISPLAY_NAME).orEmpty()
            CompleteProfileScreen(
                onBack = {
                    navController.navigateUp()
                },
                onCompleteProfileSuccess = {
                    navController.navigate(NavGraph.MAIN) {
                        popUpTo(AppRoute.CompleteProfile.routeWithArg) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                initialDisplayName = displayName,
            )
        }

        navigation(
            route = NavGraph.MAIN,
            startDestination = AppRoute.MainContainer.routeWithArgs,
        ) {
            composable(
                route = AppRoute.MainContainer.routeWithArgs,
                arguments =
                    listOf(
                        navArgument(AppRoute.MainContainer.ARG_TAB) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument(AppRoute.MainContainer.ARG_TARGET) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument(AppRoute.MainContainer.ARG_RETURN_TO_NOTIFICATIONS) {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
            ) { backStackEntry ->
                val initialTabRoute =
                    backStackEntry.arguments
                        ?.getString(AppRoute.MainContainer.ARG_TAB)
                        ?.let(Uri::decode)
                        .orEmpty()
                        .ifBlank { AppRoute.Feed.route }
                val pendingRoute =
                    backStackEntry.arguments
                        ?.getString(AppRoute.MainContainer.ARG_TARGET)
                        ?.let(Uri::decode)
                        ?.takeIf { it.isNotBlank() }
                val returnToNotifications =
                    backStackEntry.arguments?.getBoolean(AppRoute.MainContainer.ARG_RETURN_TO_NOTIFICATIONS) == true

                MainContainerScreen(
                    initialTabRoute = initialTabRoute,
                    pendingRoute = pendingRoute,
                    returnToNotifications = returnToNotifications,
                    onReturnToNotifications = {
                        if (!navController.navigateUp()) {
                            navController.navigate(AppRoute.Notifications.route) {
                                launchSingleTop = true
                            }
                        }
                    },
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
                    },
                )
            }
        }

        composable(AppRoute.Notifications.route) {
            NotificationScreen(
                onBack = {
                    if (!navController.navigateUp()) {
                        navController.navigate(NavGraph.MAIN) {
                            launchSingleTop = true
                        }
                    }
                },
                onNotificationClick = { notification ->
                    navController.navigate(notification.toMainContainerRoute(returnToNotifications = true)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(AppRoute.Assistant.route) {
            AssistantScreen()
        }
    }
}

private fun Notification.toMainContainerRoute(returnToNotifications: Boolean = false): String {
    val destination = deepLinkDestination
    val targetId = deepLinkTargetId?.takeIf { it.isNotBlank() }
    val relatedTargetId = relatedId?.takeIf { it.isNotBlank() }
    val senderTargetId = senderId?.takeIf { it.isNotBlank() }
    val billTargetId = targetId ?: relatedTargetId
    val activityTargetId = targetId ?: relatedTargetId
    val profileTargetId = targetId ?: senderTargetId ?: relatedTargetId

    return when {
        // Activity/Feed detail
        (destination == NotificationDestination.ACTIVITY_DETAIL.name ||
            destination == NotificationDestination.POST_DETAIL.name) &&
            !activityTargetId.isNullOrBlank() -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Feed.route,
                target = AppRoute.PostDetail.createRoute(activityTargetId),
                returnToNotifications = returnToNotifications,
            )
        }
        // Personal transaction detail
        destination == NotificationDestination.TRANSACTION_DETAIL.name && !targetId.isNullOrBlank() -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Personal.route,
                target = AppRoute.TransactionDetail.createRoute(targetId),
                returnToNotifications = returnToNotifications,
            )
        }
        // Spending reminders (budget alert)
        destination == NotificationDestination.SPENDING_REMINDERS.name -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Personal.route,
                target = AppRoute.SpendingReminders.route,
                returnToNotifications = returnToNotifications,
            )
        }
        destination == NotificationDestination.CATEGORY_MANAGEMENT.name -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Personal.route,
                target = AppRoute.CategoryManagement.route,
                returnToNotifications = returnToNotifications,
            )
        }
        destination == NotificationDestination.PERSONAL_PLANS.name -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Personal.route,
                target = AppRoute.PersonalPlans.route,
                returnToNotifications = returnToNotifications,
            )
        }
        destination == NotificationDestination.PERSONAL.name -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Personal.route,
                returnToNotifications = returnToNotifications,
            )
        }
        // Split bill detail
        destination == NotificationDestination.SPLIT_DETAIL.name &&
            !billTargetId.isNullOrBlank() &&
            !groupId.isNullOrBlank() -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Split.route,
                target = AppRoute.BillDetail.createRoute(groupId, billTargetId),
                returnToNotifications = returnToNotifications,
            )
        }
        // Split settle/payment
        destination == NotificationDestination.SPLIT_SETTLE.name &&
            !billTargetId.isNullOrBlank() &&
            !groupId.isNullOrBlank() -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Split.route,
                target = AppRoute.BillDetail.createRoute(groupId, billTargetId),
                returnToNotifications = returnToNotifications,
            )
        }
        // Other user profile (from activity)
        destination == NotificationDestination.PROFILE.name && !profileTargetId.isNullOrBlank() -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Feed.route,
                target = AppRoute.OtherUserProfile.createRoute(profileTargetId),
                returnToNotifications = returnToNotifications,
            )
        }
        // Fallback: route to split tab if payment/bill notification
        type in
            setOf(
                NotificationType.BILL_CREATED,
                NotificationType.PAYMENT_PENDING,
                NotificationType.PAYMENT_COMPLETED,
                NotificationType.SPLIT_COMPLETED,
            )
        -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Split.route,
                returnToNotifications = returnToNotifications,
            )
        }
        // Fallback: route to activity/feed if activity update
        type == NotificationType.ACTIVITY_UPDATE -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Feed.route,
                returnToNotifications = returnToNotifications,
            )
        }
        // Fallback: route to personal if transaction alert
        type == NotificationType.TRANSACTION_ALERT -> {
            AppRoute.MainContainer.createRoute(
                tab = AppRoute.Personal.route,
                target = AppRoute.SpendingReminders.route,
                returnToNotifications = returnToNotifications,
            )
        }
        // Default fallback
        else -> AppRoute.MainContainer.createRoute(returnToNotifications = returnToNotifications)
    }
}
