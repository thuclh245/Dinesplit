package com.example.dinesplit.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dinesplit.core.navigation.AppRoute
import com.example.dinesplit.core.navigation.BottomTab
import com.example.dinesplit.presentation.feed.CreatePostScreen
import com.example.dinesplit.presentation.feed.FeedScreen
import com.example.dinesplit.presentation.feed.PostDetailScreen
import com.example.dinesplit.presentation.feed.SearchScreen
import com.example.dinesplit.presentation.personal.PersonalScreen
import com.example.dinesplit.presentation.profile.EditProfileScreen
import com.example.dinesplit.presentation.profile.OtherUserProfileScreen
import com.example.dinesplit.presentation.profile.ProfileScreen
import com.example.dinesplit.presentation.profile.ProfileViewModel
import com.example.dinesplit.presentation.split.SplitScreen

@Composable
fun MainContainerScreen(
    onOpenNotifications: () -> Unit,
    onOpenAssistant: () -> Unit,
    onLogout: () -> Unit
) {
    val mainNavController = rememberNavController()
    val profileViewModel: ProfileViewModel = viewModel()
    val profileUiState by profileViewModel.profileUiState.collectAsState()
    val editProfileUiState by profileViewModel.editUiState.collectAsState()
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
                    onOpenAssistant = onOpenAssistant,
                    onCreatePost = { mainNavController.navigate(AppRoute.CreatePost.route) },
                    onOpenPostDetail = { postId ->
                        mainNavController.navigate(AppRoute.PostDetail.createRoute(postId))
                    },
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                    onOpenOtherUserProfile = { userName ->
                        mainNavController.navigate(AppRoute.OtherUserProfile.createRoute(userName))
                    }
                )
            }

            composable(AppRoute.CreatePost.route) {
                CreatePostScreen(onBack = { mainNavController.navigateUp() })
            }

            composable(AppRoute.Search.route) {
                SearchScreen(onBack = { mainNavController.navigateUp() })
            }

            composable(AppRoute.PostDetail.routeWithArg) { backStackEntry ->
                PostDetailScreen(
                    postId = backStackEntry.arguments?.getString(AppRoute.PostDetail.ARG_ID).orEmpty(),
                    onBack = { mainNavController.navigateUp() }
                )
            }

            composable(AppRoute.OtherUserProfile.routeWithArg) { backStackEntry ->
                OtherUserProfileScreen(
                    userName = backStackEntry.arguments?.getString(AppRoute.OtherUserProfile.ARG_USER).orEmpty(),
                    onBack = { mainNavController.navigateUp() }
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
                    uiState = profileUiState,
                    onEditProfile = { mainNavController.navigate(AppRoute.EditProfile.route) },
                    onLogout = onLogout,
                    onOpenNotifications = onOpenNotifications
                )
            }

            composable(AppRoute.EditProfile.route) {
                EditProfileScreen(
                    uiState = editProfileUiState,
                    onDisplayNameChange = profileViewModel::onDisplayNameChange,
                    onUsernameChange = profileViewModel::onUsernameChange,
                    onBioChange = profileViewModel::onBioChange,
                    onSave = profileViewModel::saveProfile,
                    onBack = { mainNavController.navigateUp() }
                )
            }
        }
    }
}
