package com.example.dinesplit.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dinesplit.core.navigation.AppRoute
import com.example.dinesplit.core.navigation.BottomTab
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.presentation.feed.FeedScreen
import com.example.dinesplit.presentation.personal.AddTransactionScreen
import com.example.dinesplit.presentation.personal.CategoryManagementScreen
import com.example.dinesplit.presentation.personal.HistoryScreen
import com.example.dinesplit.presentation.personal.PersonalRoute
import com.example.dinesplit.presentation.personal.toRouteValue
import com.example.dinesplit.presentation.personal.transactionTypeFromRoute
import com.example.dinesplit.presentation.profile.ProfileScreen
import com.example.dinesplit.presentation.split.SplitScreen

@Composable
fun MainContainerScreen(
    onOpenNotifications: () -> Unit,
    onOpenAssistant: () -> Unit
) {
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomTab.items.forEach { tab ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == tab.route } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            mainNavController.navigate(tab.route) {
                                popUpTo(mainNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            // Tạm thời lấy chữ cái đầu của label làm icon
                            Text(tab.label.take(1))
                        },
                        label = {
                            Text(tab.label)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = AppRoute.Feed.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Feed.route) {
                FeedScreen(
                    onOpenNotifications = onOpenNotifications,
                    onOpenAssistant = onOpenAssistant
                )
            }

            composable(AppRoute.Split.route) {
                SplitScreen()
            }

            composable(AppRoute.Personal.route) {
                PersonalRoute(
                    onOpenAssistant = onOpenAssistant,
                    onAddExpense = {
                        mainNavController.navigate(
                            AppRoute.AddTransaction.createRoute(TransactionType.EXPENSE.toRouteValue())
                        )
                    },
                    onAddIncome = {
                        mainNavController.navigate(
                            AppRoute.AddTransaction.createRoute(TransactionType.INCOME.toRouteValue())
                        )
                    },
                    onOpenHistory = {
                        mainNavController.navigate(AppRoute.TransactionHistory.route)
                    },
                    onOpenCategoryManagement = {
                        mainNavController.navigate(AppRoute.CategoryManagement.route)
                    }
                )
            }

            composable(
                route = AppRoute.AddTransaction.routeWithArg,
                arguments = listOf(
                    navArgument(AppRoute.AddTransaction.ARG_TYPE) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val initialType = transactionTypeFromRoute(
                    backStackEntry.arguments?.getString(AppRoute.AddTransaction.ARG_TYPE)
                )
                AddTransactionScreen(
                    onBack = { mainNavController.navigateUp() },
                    initialType = initialType
                )
            }

            composable(AppRoute.TransactionHistory.route) {
                HistoryScreen(
                    onBack = { mainNavController.navigateUp() }
                )
            }

            composable(AppRoute.CategoryManagement.route) {
                CategoryManagementScreen(
                    onBack = { mainNavController.navigateUp() }
                )
            }

            composable(AppRoute.Profile.route) {
                ProfileScreen(
                    onOpenNotifications = onOpenNotifications
                )
            }
        }
    }
}
