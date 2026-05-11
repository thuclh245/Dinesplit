package com.example.dinesplit.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
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
import com.example.dinesplit.presentation.profile.ProfileUiEffect
import com.example.dinesplit.presentation.profile.ProfileViewModel
import com.example.dinesplit.presentation.split.SplitScreen
import kotlinx.coroutines.flow.collectLatest

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
    val showBottomBar = BottomTab.items.any { tab ->
        currentDestination
            ?.hierarchy
            ?.any { it.route == tab.route } == true
    }

    LaunchedEffect(profileViewModel) {
        profileViewModel.effect.collectLatest { effect ->
            when (effect) {
                ProfileUiEffect.LogoutSuccess -> onLogout()
                ProfileUiEffect.SaveSuccess -> mainNavController.navigateUp()
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(tab.label)
                            }
                        )
                    }
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
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    onOpenNotifications = onOpenNotifications,
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                    onSettleUp = { /* Handle settle up */ }
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
                SplitScreen(
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    onOpenNotifications = onOpenNotifications,
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                    onNewGroup = { /* New Group */ },
                    onNewExpense = { /* New Expense */ }
                )
            }

            composable(AppRoute.Personal.route) {
                PersonalScreen(
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                    onAddTransaction = { /* Add Transaction */ }
                )
            }

            composable(AppRoute.Profile.route) {
                ProfileScreen(
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    userName = profileUiState.profile?.displayName ?: "Profile",
                    onEditProfile = { mainNavController.navigate(AppRoute.EditProfile.route) },
                    onOpenSettings = { /* Open Settings */ },
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) }
                )
            }

            composable(AppRoute.EditProfile.route) {
                EditProfileScreen(
                    uiState = editProfileUiState,
                    onDisplayNameChange = profileViewModel::onDisplayNameChange,
                    onUsernameChange = profileViewModel::onUsernameChange,
                    onBioChange = profileViewModel::onBioChange,
                    onAvatarChange = profileViewModel::onAvatarSelected,
                    onAvatarClear = profileViewModel::onAvatarCleared,
                    onSave = profileViewModel::saveProfile,
                    onBack = { mainNavController.navigateUp() }
                )
            }
        }
    }
}
