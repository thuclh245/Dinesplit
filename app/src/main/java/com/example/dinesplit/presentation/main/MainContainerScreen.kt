package com.example.dinesplit.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dinesplit.core.navigation.AppRoute
import com.example.dinesplit.core.navigation.BottomTab
import com.example.dinesplit.presentation.feed.FeedScreen
import com.example.dinesplit.presentation.personal.PersonalScreen
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
                PersonalScreen(
                    onOpenAssistant = onOpenAssistant
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
