package com.example.dinesplit.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
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
                onTimeout = {
                    navController.navigate(NavGraph.AUTH) {
                        popUpTo(AppRoute.Splash.route) { inclusive = true }
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
                    onLoginSuccess = {
                        navController.navigate(AppRoute.MainContainer.route) {
                            popUpTo(NavGraph.AUTH) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppRoute.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(AppRoute.CompleteProfile.route)
                    }
                )
            }

            composable(AppRoute.CompleteProfile.route) {
                CompleteProfileScreen(
                    onCompleteProfileSuccess = {
                        navController.navigate(AppRoute.MainContainer.route) {
                            popUpTo(NavGraph.AUTH) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(AppRoute.MainContainer.route) {
            MainContainerScreen(
                onOpenNotifications = {
                    navController.navigate(AppRoute.Notifications.route)
                },
                onOpenAssistant = {
                    navController.navigate(AppRoute.Assistant.route)
                }
            )
        }

        composable(AppRoute.Notifications.route) {
            NotificationScreen()
        }

        composable(AppRoute.Assistant.route) {
            AssistantScreen()
        }
    }
}
