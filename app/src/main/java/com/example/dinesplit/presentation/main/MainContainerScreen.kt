package com.example.dinesplit.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.dinesplit.presentation.split.CreateBillScreen
import com.example.dinesplit.presentation.split.GroupDetailScreen
import com.example.dinesplit.presentation.split.GroupListScreen
import com.example.dinesplit.presentation.split.SplitScreen
import com.example.dinesplit.ui.theme.DineSplitTheme
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                MainBottomBar(
                    isTabSelected = { tab ->
                        currentDestination
                            ?.hierarchy
                            ?.any { it.route == tab.route } == true
                    },
                    onTabSelected = { tab ->
                        mainNavController.navigate(tab.route) {
                            popUpTo(mainNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = AppRoute.Feed.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Fix lỗi lint và đảm bảo đúng layout
        ) {
            composable(AppRoute.Feed.route) {
                FeedScreen(
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    onOpenNotifications = onOpenNotifications,
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                    onSettleUp = { /* Handle settle up */ }
                )
            }
            composable(AppRoute.Split.route) {
                SplitScreen(
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    onOpenNotifications = onOpenNotifications,
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                    onNewGroup = { mainNavController.navigate(AppRoute.GroupList.route) },
                    onNewExpense = { mainNavController.navigate(AppRoute.CreateBill.route) },
                    onViewAllGroups = { mainNavController.navigate(AppRoute.GroupList.route) },
                    onGroupClick = { mainNavController.navigate(AppRoute.GroupDetail.route) }
                )
            }

            composable(AppRoute.GroupList.route) {
                GroupListScreen(
                    onNavigateToGroupDetail = { mainNavController.navigate(AppRoute.GroupDetail.route) },
                    onNavigateToCreateGroup = { mainNavController.navigate(AppRoute.CreateBill.route) },
                    onNavigateToAllGroups = { /* already here */ }
                )
            }

            composable(AppRoute.GroupDetail.route) {
                GroupDetailScreen(
                    onBack = { mainNavController.navigateUp() },
                    onNavigateToCreateBill = { mainNavController.navigate(AppRoute.CreateBill.route) },
                    onNavigateToBillDetail = { mainNavController.navigate(AppRoute.CreateBill.route) },
                    onNavigateToSettleSummary = { /* TODO */ }
                )
            }
            composable(AppRoute.Personal.route) {
                PersonalScreen(
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    onOpenSearch = { mainNavController.navigate(AppRoute.Search.route) },
                    onAddTransaction = { mainNavController.navigate(AppRoute.AddTransaction.route) }
                )
            }
            composable(AppRoute.Profile.route) {
                ProfileScreen(
                    userAvatarUrl = profileUiState.profile?.avatarUrl,
                    userName = profileUiState.profile?.displayName ?: "User",
                    onEditProfile = { mainNavController.navigate(AppRoute.EditProfile.route) },
                    onOpenSettings = { /* TODO: Implement settings */ },
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
            composable(AppRoute.Search.route) {
                SearchScreen(onBack = { mainNavController.navigateUp() })
            }
            composable(AppRoute.CreatePost.route) {
                CreatePostScreen(onBack = { mainNavController.navigateUp() })
            }
            composable(AppRoute.CreateBill.route) {
                CreateBillScreen(onBack = { mainNavController.navigateUp() })
            }
            composable(AppRoute.PostDetail.routeWithArg) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString(AppRoute.PostDetail.ARG_ID) ?: ""
                PostDetailScreen(postId = postId, onBack = { mainNavController.navigateUp() })
            }
            composable(AppRoute.OtherUserProfile.routeWithArg) { backStackEntry ->
                val userName = backStackEntry.arguments?.getString(AppRoute.OtherUserProfile.ARG_USER) ?: ""
                OtherUserProfileScreen(userName = userName, onBack = { mainNavController.navigateUp() })
            }
        }
    }
}

@Composable
private fun MainBottomBar(
    isTabSelected: (BottomTab) -> Boolean,
    onTabSelected: (BottomTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = Color.White,
        tonalElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomTab.items.forEach { tab ->
                    val selected = isTabSelected(tab)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabSelected(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) Color(0xFFE65100) else Color.LightGray,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (selected) Color(0xFFE65100) else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Main Bottom Bar")
@Composable
private fun MainBottomBarPreview() {
    DineSplitTheme {
        MainBottomBar(
            isTabSelected = { tab -> tab.route == AppRoute.Feed.route },
            onTabSelected = {}
        )
    }
}
